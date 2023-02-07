(ns app.common.leaflet
  (:require [reagent.core :as reagent :refer [atom]]
            [app.util :refer (deep-merge)]
            [re-frame.core :as rf :refer
             (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
            [leaflet :as L]))

;;;;;;;;;
;; Inspired by https://github.com/tatut/reagent-leaflet
;; Define the React lifecycle callbacks to manage the LeafletJS
;; Javascript objects.

(declare update-leaflet-geometries)
(declare leaflet-did-mount)
(declare leaflet-render)

;;;;;;;;;
;; The LeafletJS Reagent component.

(defn leaflet [{:keys [id style view zoom layers geometries] :as mapspec}]
  "A LeafletJS map component."
  (let [leaflet (atom nil)
        layers
        (or layers
            [{:type :tile
              :url "http://{s}.tile.osm.org/{z}/{x}/{y}.png"
              :attribution "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors"}])
        geometries-map (atom {})
        mapspec (deep-merge
                 mapspec
                 {:leaflet leaflet
                  :layers layers
                  :geometries-map geometries-map})]
    (reagent/create-class
     {:component-did-mount (leaflet-did-mount mapspec)
      #_#_:component-will-update (leaflet-will-update mapspec)
      :reagent-render (leaflet-render mapspec)})))



(defn- leaflet-did-mount [{:keys [id view zoom layers leaflet geometries] :as mapspec}]
  "Initialize LeafletJS map for a newly mounted map component."
  (fn []
    ;; Initialize leaflet map
    (reset! leaflet (L/map id (clj->js {:zoomControl false})))

    ;; Initial view point and zoom level
    (.setView @leaflet (clj->js @view) @zoom)

    #_(.addTo (L/polyline (clj->js [[65.19966161643839 25.39832800626755]
                                    [65.4 25.5]])
                          #js {:color "blue"})
              @leaflet)

    ;; Initialize map layers
    (doseq [{:keys [type url] :as layer-spec} layers]
      (let [layer (case type
                    :tile (L/tileLayer
                           url
                           (clj->js {:attribution (:attribution layer-spec)}))
                    :wms (L/tileLayer.wms
                          url
                          (clj->js {:format "image/png"
                                    :fillOpacity 1.0})))]
        (.addTo layer @leaflet)))

    ;; If mapspec defines callbacks, bind them to leaflet
    #_(when-let [on-click (:on-click mapspec)]
        (.on leaflet "click" (fn [e]
                               (on-click [(-> e .-latlng .-lat) (-> e .-latlng .-lng)]))))

    ;; Add callback for leaflet pos/zoom changes
    ;; watcher for pos/zoom atoms
    (.on @leaflet "move" (fn [e]
                           (let [c (.getCenter @leaflet)]
                             (reset! zoom (.getZoom @leaflet))
                             (reset! view [(.-lat c) (.-lng c)]))))
    (add-watch view ::view-update
               (fn [_ _ old-view new-view]
                 ;;(.log js/console "change view: " (clj->js old-view) " => " (clj->js new-view) @zoom)
                 (when (not= old-view new-view)
                   (.setView @leaflet (clj->js new-view) @zoom))))
    (add-watch zoom ::zoom-update
               (fn [_ _ old-zoom new-zoom]
                 (when (not= old-zoom new-zoom)
                   (.setZoom @leaflet new-zoom))))
    ;; If the mapspec has an atom containing geometries, add watcher
    ;; so that we update all LeafletJS objects
    (update-leaflet-geometries mapspec @geometries)
    (when-let [g geometries]
      (add-watch g ::geometries-update
                 (fn [_ _ _ new-geometries]
                   (.log js/console "Fire Watch!")
                   (update-leaflet-geometries mapspec new-geometries))))))


(defn- leaflet-render [{:keys [id style geometries] :as mapspec}]
  (fn []
    #_(let [g @geometries])
    [:div {:id id
           :style style}]))

;;;;;;;;;;
;; Code to sync ClojureScript geometries vector data to LeafletJS
;; shape objects.

(defmulti create-shape :type)

(defmethod create-shape :polygon [{:keys [coordinates]}]
  (L/polygon (clj->js coordinates)
             #js {:color "red"
                  :fillOpacity 0.5}))

(defmethod create-shape :line [{:keys [coordinates leaflet args]}]
  (L/polyline (clj->js coordinates)
              (clj->js (merge {:color "blue"} args))))

(defmethod create-shape :point [{:keys [coordinates leaflet]}]
  (L/circle (clj->js (first coordinates))
            10
            #js {:color "green"}))

(def icon (L/icon (clj->js {:iconUrl "img/bank-icon.svg"
                            :iconAnchor [20 29]
                            :iconSize [40 40]})))

(defmethod create-shape :marker [{:keys [coordinates leaflet event-handlers]}]
  (let [shape
        (L/marker (clj->js coordinates) #js {:icon icon})
        transform-event
        (fn [e f] (fn [s] (.on s e f)))
        event-list
        (map
         (fn [[e f]] (transform-event (name e) f))
         event-handlers)
        attach-events
        (apply comp event-list)]
    (attach-events shape))
  #_(-> (L/marker (clj->js coordinates) #js {:icon icon})
        #_(.on "click" (fn [] (.log js/console "clicked")))
        #_(.on "click"
               (fn [e]
                 (-> (L/popup)
                     (.setLatLng (.-latlng e))
                     (.setContent "Popup")
                     (.openOn leaflet))))))

(defmethod create-shape :inst-marker [{:keys [coordinates leaflet name]}]
  (-> (L/marker (clj->js coordinates) #js {:icon icon})
      #_(.bindPopup "t")
      #_(.openPopup)
      (.on "click"
           (fn [e]
             (-> (L/popup (clj->js {:offset [0 -32]}))
                 (.setLatLng (.-latlng e))
                 (.setContent name)
                 (.openOn leaflet))))))


(defn- update-leaflet-geometries [mapspec geometries]
  "Update the LeafletJS layers based on the data, mutates the LeafletJS map object."
  (let [{:keys [leaflet geometries-map]} mapspec
        geometries-set (into #{} geometries)]
    ;; Remove all LeafletJS shape objects that are no longer in the new geometries
    (doseq [removed (keep (fn [[geom shape]]
                            (when-not (geometries-set geom)
                              shape))
                          @geometries-map)]
      (.removeLayer @leaflet removed))

    ;; Create new shapes for new geometries and update the geometries map
    (loop [new-geometries-map {}
           [geom & geometries] geometries]
      (if-not geom
        ;; Update component state with the new geometries map
        (reset! geometries-map new-geometries-map)
        (if-let [existing-shape (@geometries-map geom)]
          ;; Have existing shape, don't need to do anything
          (recur (assoc new-geometries-map geom existing-shape) geometries)

          ;; No existing shape, create a new shape and add it to the map
          (let [shape (create-shape (merge {:leaflet @leaflet} geom))]
            (.addTo shape @leaflet)
            (recur (assoc new-geometries-map geom shape) geometries)))))))