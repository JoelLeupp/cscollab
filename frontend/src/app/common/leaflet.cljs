(ns app.common.leaflet
  (:require [reagent.core :as reagent :refer [atom]]
            [app.util :refer (deep-merge)]
            [reagent.dom.server :refer [render-to-string]]
            [app.components.colors :refer [colors]]
            [app.cscollab.transformer :as tf]
            [leaflet :as L]
            [app.cscollab.map-panel :as mp]
            [app.db :as db]
            [leaflet-ellipse]
            [re-frame.core :as rf :refer
             (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]))


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
 ::selected-records
 :<- [::selected-shape]
 :<- [::tf/filtered-collab]
 :<- [::mp/insti?]
 (fn
   [[selected-shape filtered-collab insti?]]
   "get all records of the selected shape"
   (when filtered-collab
     (let [node-m (if insti? :a_inst :a_pid)
           node-n (if insti? :b_inst :b_pid)]
       (if (vector? selected-shape)
       ;; get records of selected collaboration
         (filter
          #(or
            (and (= (first selected-shape) (node-n %))
                 (= (second selected-shape) (node-m %)))
            (and (= (first selected-shape) (node-m %))
                 (= (second selected-shape) (node-n %))))
          filtered-collab)
        ;; get collaborations of author or institution
         (filter
          #(or (= selected-shape (node-m %))
               (= selected-shape (node-n %)))
          filtered-collab))))))


(reg-sub
 ::info-open?
 :<- [::leaflet-field :info-open?]
 (fn [m] (when m m)))



(comment
  @(subscribe [::zoom-level])
  (first @(subscribe [::tf/filtered-collab]))
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
        insti? (subscribe [::mp/insti?])
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
                  :insti? insti?
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


(declare color-selected)
(declare uncolor-previous)
(declare color-all-main)

(defn- leaflet-did-mount [{:keys [id view zoom layers leaflet geometries geometries-map insti?] :as mapspec}]
  "Initialize LeafletJS map for a newly mounted map component."
  (fn []
    ;; Initialize leaflet map 
    (reset! leaflet (L/map id (clj->js {:zoomControl false})))
    (reset! geometries-map {})
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
    (.on @leaflet
         "click" (fn [e] 
                   (when @(subscribe [::info-open?])
                     (color-all-main geometries-map @insti?)
                     (dispatch [::set-leaflet [:info-open?] false])
                     (dispatch [::set-leaflet [:selected-shape] nil]))))

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
    (add-watch (subscribe [::selected-records]) ::color-selected
               (fn [_ _ previous-selected _]
                 (when previous-selected
                   (uncolor-previous previous-selected geometries-map @insti?))
                 (color-selected geometries-map @insti?)))
    ;; If the mapspec has an atom containing geometries, add watcher
    ;; so that we update all LeafletJS objects
    (update-leaflet-geometries mapspec @geometries)
    (when-let [g geometries]
      (add-watch g ::geometries-update
                 (fn [_ _ _ new-geometries] 
                   (update-leaflet-geometries mapspec new-geometries))))))


(defn- leaflet-render [{:keys [id style view zoom geometries] :as mapspec}]
  (fn []
    (let [g @geometries
          selected-records @(subscribe [::selected-records])] 
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
                 :fillColor (:main colors)
                 :weight weight
                 :radius radius
                 :fillOpacity 1}))

(defmethod create-shape :ellipse [{:keys [coordinates radii tilt id weight leaflet]}]
  (-> (L/ellipse (clj->js coordinates)  (clj->js radii) tilt
                 #js {:color (:main colors) 
                      :weight weight 
                      :fillOpacity 0})
      (.on "click" (fn [e]
                     (dispatch [::set-leaflet [:info-open?] true])
                     (dispatch [::set-leaflet [:selected-shape] id])))))

(defn calc-offset [[x y]]
  [(* 0.5 x) (* 0.5 y)]
  #_[(* (/ 33 60) x) (* (/ 22 60) y)])

(defn icon [scale]
  (let [size (* scale 20)
        offset (calc-offset [size size])]
    (L/icon (clj->js {:iconUrl "img/inst-icon-v2.svg" #_"img/inst-icon.svg" ;https://uxwing.com/svg-icon-editor
                      :iconAnchor offset #_[33 22] #_[20 29]
                      :iconSize [size size] #_[40 40]}))))

(defn inst-svg [size color]
  (render-to-string 
   [:svg {:width size :fill color :height size :version "1.1" :viewBox "0 0 500 500"
          :xmlns "http://www.w3.org/2000/svg" :preserveAspectRatio "none"}
    [:g
     [:g {:transform "rotate(-0.0563197 250 250)"}
      [:circle {:cx "250" :cy "271.94428" :r "219.44287" :fill "white"}]
      [:path {:d "m499.88361,249.99999c0,-66.20152 -26.3262,-129.69512 -73.18789,-176.50668c-46.86169,-46.81155 -110.42329,-73.10959 -176.69572,-73.10959s-129.83403,26.29803 -176.69572,73.10959c-46.86169,46.81155 -73.18789,110.30515 -73.18789,176.50668s26.3262,129.69512 73.18789,176.50668c46.86169,46.81155 110.42329,73.10959 176.69572,73.10959s129.83403,-26.29803 176.69572,-73.10959c46.86169,-46.81155 73.18789,-110.30515 73.18789,-176.50668zm-403.65814,76.805c0,-10.60348 8.60698,-19.20125 19.22182,-19.20125l19.22182,0l0,-110.98104l-25.17728,12.86659c-2.67743,1.36294 -5.64638,2.09158 -8.64982,2.10871c-8.89871,0.03428 -16.65598,-6.02612 -18.75852,-14.65813c-2.10238,-8.632 1.99942,-17.5811 9.91121,-21.63575l148.19691,-76.805l3.45817,-1.53439l0.00859,0c4.08469,-1.52581 8.59841,-1.52581 12.68288,0l3.45817,1.53439l148.19691,76.805l0.00857,0c7.91192,4.05465 12.01375,13.00375 9.91121,21.63575c-2.10238,8.632 -9.8598,14.69236 -18.75852,14.65813c-3.00344,-0.01714 -5.9726,-0.74575 -8.64982,-2.10871l-25.17728,-12.86659l0,110.98104l19.22182,0c10.61484,0 19.22182,8.59777 19.22182,19.20125s-8.60698,19.20125 -19.22182,19.20125l-269.10543,0c-10.61484,0 -19.22182,-8.59777 -19.22182,-19.20125l0.00042,0z"}]
      [:path {:d "m173.11273,176.83774l0,130.75723l19.22182,0l0,-115.20751c0,-10.60348 8.60698,-19.20125 19.22182,-19.20125s19.22182,8.59777 19.22182,19.20125l0,115.20751l38.44363,0l0,-115.20751c0,-10.60348 8.60698,-19.20125 19.22182,-19.20125s19.22182,8.59777 19.22182,19.20125l0,115.20751l19.22182,0l0,-130.75723l-76.88727,-39.55019l-76.88727,39.55019z"}]]]]))


(defn author-svg [size color]
  (render-to-string
   [:svg {:width size :fill color :height size :version "1.1" :viewBox "0 0 500 500"
          :xmlns "http://www.w3.org/2000/svg" :preserveAspectRatio "none"}
    [:g 
     [:circle {:cx "250" :cy "250" :r "200" :fill "white"}]
     [:path {:d "m0,0l24,0l0,24l-24,0l0,-24z" :fill "none"}]
     [:path {:fill color :stroke nil :d "m250.99999,2c-136.896,0 -247.99999,110.87999 -247.99999,247.49998s111.104,247.49998 247.99999,247.49998s247.99999,-110.87999 247.99999,-247.49998s-111.104,-247.49998 -247.99999,-247.49998zm0,74.24999c41.168,0 74.4,33.165 74.4,74.24999s-33.232,74.24999 -74.4,74.24999s-74.4,-33.165 -74.4,-74.24999s33.232,-74.24999 74.4,-74.24999zm0,351.44997a178.55999,178.19999 0 0 1 -148.8,-79.69499c0.744,-49.2525 99.2,-76.22999 148.8,-76.22999c49.352,0 148.056,26.9775 148.8,76.22999a178.55999,178.19999 0 0 1 -148.8,79.69499z"}]]]))

(defn gen-icon [scale color svg]
  (let [size (* scale 20)
        offset (calc-offset [size size])]
    (L/divIcon 
     (clj->js {:html (svg size (or color (:main colors)))
               :className ""
               :iconAnchor offset
               :iconSize [size size]}))))


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


(defn uncolor-previous [previous-selected geometries-map insti?]
  (let [records previous-selected
        svg (if insti? inst-svg author-svg)
        node-n (if insti? :a_inst :a_pid)
        node-m (if insti? :b_inst :b_pid)
        markers
        (map #(get @geometries-map %)
             (clojure.set/union
              (set (map node-n records))
              (set (map node-m records))))
        lines
        (map #(get @geometries-map %)
             (set (map #(identity [(node-n %) (node-m %)]) records)))]
    (doseq [layer markers]
      (let [icon (gen-icon (.-scale layer) (:main colors) svg)]
        (.setIcon layer icon)))
    (doseq [layer lines]
      (.setStyle layer (clj->js {:color (:main colors)})))))

(defn color-selected [geometries-map insti?]
  (let [records @(subscribe [::selected-records])
        svg (if insti? inst-svg author-svg)
        node-n (if insti? :a_inst :a_pid)
        node-m (if insti? :b_inst :b_pid)
        markers
        (map #(get @geometries-map %)
             (clojure.set/union
              (set (map node-n records))
              (set (map node-m records))))
        lines
        (map #(get @geometries-map %)
             (set (map #(identity [(node-n %) (node-m %)]) records)))]
    (doseq [layer markers]
      (let [icon (gen-icon (.-scale layer) (:second colors) svg)]
        (.setIcon layer icon)))
    (doseq [layer lines]
      (.setStyle layer (clj->js {:color (:second colors)})))))

(defn color-all-main [geometries-map insti?]
  (let [svg (if insti? inst-svg author-svg)
        markers
        (map second (filter #(string? (first %)) @geometries-map))
        lines
        (map second (filter #(vector? (first %)) @geometries-map))]
    (doseq [layer markers]
      (let [icon (gen-icon (.-scale layer) (:main colors) svg)]
        (.setIcon layer icon)))
    (doseq [layer lines]
      (.setStyle layer (clj->js {:color (:main colors)})))))

(defmethod create-shape :inst-marker [{:keys [coordinates scale id leaflet name]}]
  (let [i-icon (gen-icon scale (:main colors) inst-svg) #_(icon scale)
        marker (L/marker (clj->js coordinates) #js {:icon i-icon})] 
    (set! (.-scale marker) scale)
    (-> marker
        #_(.bindPopup "t")
        #_(.openPopup) 
        (.on "click"
             (fn [e]
               (dispatch [::set-leaflet [:selected-shape] id])
               (dispatch [::set-leaflet [:info-open?] true])
               ))
        #_(.on "mouseover"
               (fn [e]
                 (-> (L/popup (clj->js {:offset [0 -10]}))
                     (.setLatLng (.-latlng e))
                     (.setContent name)
                     (.openOn leaflet)))))))

(defmethod create-shape :author [{:keys [coordinates scale id leaflet]}]
  (let [i-icon (gen-icon scale (:main colors) author-svg) #_(icon scale)
        marker (L/marker (clj->js coordinates) #js {:icon i-icon})]
    (set! (.-scale marker) scale)
    (-> marker
        (.on "click"
             (fn [e]
               (dispatch [::set-leaflet [:selected-shape] id])
               (dispatch [::set-leaflet [:info-open?] true])))))
  
  #_(L/circle (clj->js coordinates)
            #js {:color (:main colors)
                 :fillColor (:main colors)
                 :radius (* 100 scale)
                 :fillOpacity 1}))

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
    (doseq [removed (vals @geometries-map) #_(keep (fn [[id shape]]
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
        (let [shape (create-shape (merge {:leaflet @leaflet} geom))]
          (.addTo shape @leaflet)
          (recur (assoc new-geometries-map (:id geom) shape) geometries))
        #_(if-let [existing-shape (@geometries-map (:id geom))]
          ;; Have existing shape, don't need to do anything
          (recur (assoc new-geometries-map (:id geom) existing-shape) geometries)

          ;; No existing shape, create a new shape and add it to the map
          (let [shape (create-shape (merge {:leaflet @leaflet} geom))]
            (.addTo shape @leaflet)
            (recur (assoc new-geometries-map (:id geom) shape) geometries)))))))