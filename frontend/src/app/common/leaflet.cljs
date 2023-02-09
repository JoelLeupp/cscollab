(ns app.common.leaflet 
  (:require [reagent.core :as reagent :refer [atom]]
            [app.util :refer (deep-merge)] 
            [reagent.dom.server :refer [render-to-string]]
            [app.components.colors :refer [colors]]
            [re-frame.core :as rf :refer
             (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
            [leaflet :as L]))

;;;;;;;;;;;;;
;; define events and subscriptions for the leaflet component


(reg-sub
 ::leaflet
 (fn [db _] (:leaflet db)))

(reg-sub
 ::leaflet-field
 :<- [::leaflet]
 (fn [m  [_ id]]
   (let [id (if (vector? id) id [id])]
     (get-in m id))))

(reg-event-fx
 ::set-leaflet
 (fn [{db :db} [_ id value]]
   (let [id (if (vector? id) id [id])]
     {:db (assoc-in db (into [:leaflet] id) value)})))

(reg-event-fx
 ::update-leaflet
 (fn [{db :db} [_ id f]]
   (let [id (if (vector? id) id [id])]
     {:db (update-in db (into [:leaflet] id) f)})))

(reg-sub
 ::view-position
 :<- [::leaflet-field :view-position]
 (fn [p] (when p p)))

(reg-sub
 ::zoom-level
 :<- [::leaflet-field :zoom-level]
 (fn [z] (when z z)))

(reg-sub
 ::geometries
 :<- [::leaflet-field :geometries]
 (fn [g] (when g g)))

(reg-sub
 ::map
 :<- [::leaflet-field :map]
 (fn [m] (when m m)))

(reg-sub
 ::selected-shape
 :<- [::leaflet-field :selected-shape]
 (fn [m] (when m m)))

(reg-sub
 ::info-open?
 :<- [::leaflet-field :info-open?]
 (fn [m] (when m m)))


(comment
  @(subscribe [::zoom-level])
  @(subscribe [::geometries])
  (dispatch [::set-leaflet [:zoom-level] 10])
  @(subscribe [::leaflet])
  (dispatch [::set-leaflet [:view-position] [50 50]])
  (.setView @(subscribe [::map]) (clj->js [50 50]) 6)
  (.getCenter @(subscribe [::map])))


;;;;;;;;;;;;;
;; Define the React lifecycle callbacks to manage the LeafletJS

(declare update-leaflet-geometries)
(declare leaflet-did-mount)
(declare leaflet-render)

(defn leaflet-map [{:keys [id style layers zoom view geometries-map] :as mapspec}]
  "A LeafletJS Reagent component"
  (let [leaflet #_(subscribe [::map]) (atom nil) 
        geometries (subscribe [::geometries])
        layers
        (or layers
            [{:type :tile
              :url "http://{s}.tile.osm.org/{z}/{x}/{y}.png"
              :attribution "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors"}])
        geometries-map geometries-map
        mapspec (deep-merge
                 mapspec
                 {:leaflet leaflet
                  :view view
                  :zoom zoom
                  :layers layers
                  :geometries geometries
                  :geometries-map geometries-map})]
    (fn []
      (when (and @view @zoom @geometries)
        (reagent/create-class
         {:component-did-mount (leaflet-did-mount mapspec)
          #_#_:component-will-update (leaflet-will-update mapspec)
          :reagent-render (leaflet-render mapspec)})))))



(defn- leaflet-did-mount [{:keys [id view zoom layers leaflet geometries] :as mapspec}]
  "Initialize LeafletJS map for a newly mounted map component."
  (fn []
    ;; Initialize leaflet map 
    (reset! leaflet (L/map id (clj->js {:zoomControl false})))
    (dispatch [::set-leaflet [:map] @leaflet])

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
    (.on @leaflet "click" (fn [e] 
                            (when @(subscribe [::info-open?])
                             (dispatch [::set-leaflet [:info-open?] false]))))

    ;; Add callback for leaflet pos/zoom changes
    ;; watcher for pos/zoom atoms
    (.on @leaflet "move" (fn [e]
                           (let [c (.getCenter @leaflet)] 
                             (reset! zoom (.getZoom @leaflet))
                             (reset! view [(.-lat c) (.-lng c)]))))
    #_(add-watch view ::view-update
                 (fn [_ _ old-view new-view]
                 ;;(.log js/console "change view: " (clj->js old-view) " => " (clj->js new-view) @zoom)
                   (when-not (= old-view new-view)
                     (.setView @leaflet (clj->js new-view) @zoom))))
    #_(add-watch zoom ::zoom-update
                 (fn [_ _ old-zoom new-zoom]
                   (when-not (= old-zoom new-zoom)
                     (.setZoom @leaflet new-zoom))))
    ;; If the mapspec has an atom containing geometries, add watcher
    ;; so that we update all LeafletJS objects
    (update-leaflet-geometries mapspec @geometries)
    (when-let [g geometries]
      (add-watch g ::geometries-update
                 (fn [_ _ _ new-geometries]
                   (.log js/console "Fire Watch!")
                   (update-leaflet-geometries mapspec new-geometries))))))


(defn- leaflet-render [{:keys [id style view zoom geometries] :as mapspec}]
  (fn []
    (let [g @geometries] 
      [:div {:id id
             :style style}])))

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
              (clj->js (merge {:color (:main colors)} args))))

(defmethod create-shape :point [{:keys [coordinates radius weight leaflet]}]
  (L/circle (clj->js coordinates) 
            #js {:color (:main colors)
                 :weight weight
                 :radius radius
                 :fillOpacity 0}))

(defn calc-offset [[x y]]
  [(* 0.5 x) (* 0.5 y)]
  #_[(* (/ 33 60) x) (* (/ 22 60) y)])

(defn icon [scale]
  (let [size (* scale 20)
        offset (calc-offset [size size])]
    (L/icon (clj->js {:iconUrl "img/inst-icon-v2.svg" #_"img/inst-icon.svg" ;https://uxwing.com/svg-icon-editor
                      :iconAnchor offset #_[33 22] #_[20 29]
                      :iconSize [size size] #_[40 40]}))))

(defn icon-svg [color]
  (render-to-string 
   [:svg {:width "200pt" :fill color :height= "200pt" :viewBox "0 0 700 700" :version "1.0" :preserveAspectRation "none"
          :xmlns "http://www.w3.org/2000/svg" :xmlns:xlink "http://www.w3.org/1999/xlink"}
    [:g
     [:circle {:cx "350" :cy "220" :r "100" :fill "white"}]
     [:path {:d "m463.75 210c0-30.168-11.984-59.102-33.316-80.434-21.332-21.332-50.266-33.316-80.434-33.316s-59.102 
                  11.984-80.434 33.316c-21.332 21.332-33.316 50.266-33.316 80.434s11.984 59.102 33.316 80.434c21.332
                  21.332 50.266 33.316 80.434 33.316s59.102-11.984 80.434-33.316c21.332-21.332 33.316-50.266 
                  33.316-80.434zm-183.75 35c0-4.832 3.918-8.75 8.75-8.75h8.75v-50.574l-11.461 5.8633c-1.2188 
                  0.62109-2.5703 0.95313-3.9375 0.96094-4.0508 0.015625-7.582-2.7461-8.5391-6.6797-0.95703-3.9336 
                  0.91016-8.0117 4.5117-9.8594l67.461-35 1.5742-0.69922h0.003906c1.8594-0.69531 3.9141-0.69531 5.7734 
                  0l1.5742 0.69922 67.461 35h0.003906c3.6016 1.8477 5.4688 5.9258 4.5117 9.8594-0.95703 3.9336-4.4883 
                  6.6953-8.5391 6.6797-1.3672-0.007812-2.7188-0.33984-3.9375-0.96094l-11.461-5.8633v50.574h8.75c4.832
                  0 8.75 3.918 8.75 8.75s-3.918 8.75-8.75 8.75h-122.5c-4.832 0-8.75-3.918-8.75-8.75z"}]
     [:path {:d "m315 176.66v59.586h8.75v-52.5c0-4.832 3.918-8.75 8.75-8.75s8.75 3.918 8.75 8.75v52.5h17.5v-52.5c0-4.832
                  3.918-8.75 8.75-8.75s8.75 3.918 8.75 8.75v52.5h8.75v-59.586l-35-18.023z"}]]]))


(defn inst-icon [color]
  (L/divIcon 
   (clj->js {:html (icon-svg (or color (:main colors)))
             :className ""
             :iconAnchor [33 22]
             :iconSize [60 60]})))

#_(set! (.. icon -options -iconSize) (clj->js [200 200]))

(defmethod create-shape :marker [{:keys [coordinates leaflet event-handlers]}]
  (let [shape
        (L/marker (clj->js coordinates))
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

(defmethod create-shape :inst-marker [{:keys [coordinates scale id leaflet name]}]
  (let [i-icon (icon scale)]
    (-> (L/marker (clj->js coordinates) #js {:icon i-icon})
        #_(.bindPopup "t")
        #_(.openPopup)
        (.on "click" (fn [e]
                       (dispatch [::set-leaflet [:selected-shape] id])
                       (dispatch [::set-leaflet [:info-open?] true])))
        (.on "mouseover"
             (fn [e]
               (-> (L/popup (clj->js {:offset [0 -10]}))
                   (.setLatLng (.-latlng e))
                   (.setContent name)
                   (.openOn leaflet)))))))

(defmethod create-shape :collab-line [{:keys [coordinates id leaflet args]}]
  (-> (L/polyline (clj->js coordinates)
                  (clj->js (merge {:color (:main colors)} args)))
      (.on "click" (fn [e]
                     (dispatch [::set-leaflet [:info-open?] true])
                     (dispatch [::set-leaflet [:selected-shape] id])))))


(defn- update-leaflet-geometries [mapspec geometries]
  "Update the LeafletJS layers based on the data, mutates the LeafletJS map object."
  (let [{:keys [leaflet geometries-map]} mapspec
        geometries-set (into #{} (map :id geometries))]
    ;; Remove all LeafletJS shape objects that are no longer in the new geometries
    (doseq [removed (keep (fn [[id shape]]
                            (when-not (geometries-set id)
                              shape))
                          @geometries-map)]
      (.removeLayer @leaflet removed))

    ;; Create new shapes for new geometries and update the geometries map
    (loop [new-geometries-map {}
           [geom & geometries] geometries]
      (if-not geom
        ;; Update component state with the new geometries map
        (reset! geometries-map new-geometries-map)
        (if-let [existing-shape (@geometries-map (:id geom))]
          ;; Have existing shape, don't need to do anything
          (recur (assoc new-geometries-map (:id geom) existing-shape) geometries)

          ;; No existing shape, create a new shape and add it to the map
          (let [shape (create-shape (merge {:leaflet @leaflet} geom))]
            (.addTo shape @leaflet)
            (recur (assoc new-geometries-map (:id geom) shape) geometries)))))))