(ns app.cscollab.interactive-map
  (:require [reagent.core :as reagent :refer [atom]]
            [app.common.leaflet :as ll :refer [leaflet-map inst-icon]]
            [app.cscollab.transformer :as tf]
            [app.cscollab.data :as data]
            [app.components.colors :refer [colors]]
            [app.components.lists :refer [collapse]]
            [app.db :as db]
            [app.components.button :as button]
            [reagent-mui.material.paper :refer [paper]]
            [leaflet :as L]
            [app.cscollab.map-info :as info :refer (map-info-div)]
            [re-frame.core :refer
             (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]))



(defn linear-scale [min-w max-w w]
  "scale for weights between 1 and 2 based on min and max of all weights"
  (let [slope (/ 1 (- max-w min-w))
        shift (- 1 (* min-w slope))]
    (+ shift (* slope w))))

(defn gen-nodes [weighted-collab geo-mapping]
  (let [nodes (vec (clojure.set/union
                    (set (map :node/m weighted-collab))
                    (set (map :node/n weighted-collab))))
        weights
        (map
         #(reduce
           +
           (map :weight (filter (fn [node]
                                  (or
                                   (= % (:node/m node))
                                   (= % (:node/n node)))) weighted-collab)))
         nodes)
        node->weight
        (zipmap nodes weights)
        min-weight (apply min weights)
        max-weight (apply max weights)]
    ;; temorary remove nil because of utf8 string conflict
    (vec
     (remove
      nil?
      (mapv
       #(let [node-data (get geo-mapping %)]
          (when node-data
            (hash-map :type :inst-marker
                      :id (:id node-data)
                      :name (:name node-data)
                      :scale
                      (linear-scale min-weight max-weight (get node->weight (:id node-data)))
                      :coordinates (:coord node-data))))
       nodes)))))

(defn get-line-weight [w]
  (max 0.1 (min 10 (* 0.1 w))))

(defn gen-edges [weighted-collab geo-mapping]
    ;; temorary remove nil because of utf8 string conflict
  (vec (remove nil?
               (mapv
                #(let [node-m (get geo-mapping (:node/m %))
                       node-n (get geo-mapping (:node/n %))]
                   (when (and node-m node-n)
                     (if (= node-m node-n)
                       (let [lat-ellipse (- (first (:coord node-m)) 0.01)
                             lon-ellipse (- (second (:coord node-m)) 0.01)
                             lat-diff-km (* 111320 0.01) ; 1° of latitude =  111.32 km
                             lon-diff-km (js/Math.abs (/ (* (* 40075000 0.01) (js/Math.cos lat-ellipse)) 360)) ; 1° of longitude = 40075 km * cos( latitude ) / 360
                             tilt  (/ (* 180 (js/Math.atan (/ lat-diff-km lon-diff-km))) js/Math.PI)
                             radius lat-diff-km #_(js/Math.sqrt (+ (js/Math.pow lat-diff-km 2) (js/Math.pow lon-diff-km 2)))]
                         (js/console.log (str "lat: " lat-diff-km " lon: " lon-diff-km " tilt " tilt))
                         (hash-map :type :ellipse
                                   :tilt 90
                                   :radius (/ 111320 100)
                                   :radii [radius (/ radius 2)]
                                   :weight (get-line-weight (:weight %))
                                   :id [(:id node-m) (:id node-n)]
                                   :coordinates
                                   [lat-ellipse
                                    (second (:coord node-m))]))
                       (hash-map :type :collab-line
                                 :args 
                                 {:weight (get-line-weight (:weight %))}
                                 :id [(:id node-m) (:id node-n)] 
                                 :coordinates [(:coord node-m)
                                               (:coord node-n)]))))
                weighted-collab))))


(defn gen-geometries [{:keys [inst?]}]
  (let [weighted-collab
        (tf/weighted-collab {:inst? inst?})
        csauthors
        @(subscribe [::data/csauthors])
        geo-mapping
        (zipmap (map (if inst? :institution :pid) csauthors)
                (map #(hash-map :coord [(:lat %) (:lon %)]
                                :id ((if inst? :institution :pid) %)
                                :name ((if inst? :institution :name) %)) csauthors))]
    (if (and weighted-collab csauthors geo-mapping)
      (vec
       (concat 
        (gen-edges weighted-collab geo-mapping)
        (gen-nodes weighted-collab geo-mapping)))
      [])))

;; define and view atoms for leaflet component
(defonce zoom (atom 6))
(defonce view (atom [49.8 13.1]))
(defonce geometries-map (atom {}))


(defn map-comp []
  (let [geometries (subscribe [::ll/geometries])] 
    (fn []
      (when (empty? @geometries)
        (dispatch [::ll/set-leaflet [:geometries] (gen-geometries {:inst? true})]))
      [leaflet-map
       {:id "interactive-map"
        :zoom zoom
        :view view
        :geometries-map geometries-map
        :layers
        [{:type :tile
          :url "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
          :attribution "&copy; <a href=\"http://www.openstreetmap.org/copyright\">OpenStreetMap</a>"}]
        :style {:width "100%" :height "70vh" :z-index 1}}])))


(defn interactive-map [] 
  (fn [] 
    [:<>
     [paper {:elevation 1}
      [:<>
       [:div {:style {:display :flex :justify-content :space-between :background-color :white
                      :paddin-top 0 :padding-left 20 :padding-right 20}}
        [:h1 {:style {:margin 10}} "Landscape of Scientific Collaborations"]
        [button/update-button
         {:on-click #(dispatch [::ll/set-leaflet [:geometries] (gen-geometries {:inst? true})])
          :style {:z-index 999}}]]
       [map-info-div] 
       [map-comp]]]
     ]))


(comment
  @geometries-map
  (ll/color-selected geometries-map)
  (def selected-shape-ids
    (let [records @(subscribe [::info/selected-records])]
      (clojure.set/union
       (set (map #(identity [(:a_inst %) (:b_inst %)]) records))
       (set (map :a_inst records))
       (set (map :b_inst records)))))
  (def selected-markers
    (map #(get @geometries-map %) (filter string? selected-shape-ids)))
  (def selected-lines
    (map #(get @geometries-map %) (filter vector? selected-shape-ids)))

  (defn new-icon [size]
    (.extend L/Icon. (clj->js {:options {:iconUrl "img/inst-icon.svg"
                                         :iconAnchor (calc-offset [size size]) #_[20 29]
                                         :iconSize [size size]}})))
  
  (.-scale (first selected-markers))
  (js/console.log (first selected-markers))

  (doseq [layer selected-markers]
    (let [icon (inst-icon (.-scale layer) (:second colors))] 
      (.setIcon layer icon)))
  
  (doseq [layer selected-lines] 
    (.setStyle layer (clj->js {:color (:second colors)})))

  @geometries-map
  (def markers (map second (filter #(string? (first %)) @geometries-map)))

  (defn calc-offset [[x y]]
    [(* (/ 33 60) x) (* (/ 22 60) y)])

  (defn new-icon [size]
    (.extend L/Icon. (clj->js {:options {:iconUrl "img/inst-icon.svg"
                                         :iconAnchor (calc-offset [size size]) #_[20 29]
                                         :iconSize [size size]}})))
  (doseq [layer markers]
    (let [icon (new-icon 90)]
      (.setIcon layer (new icon))))


  (.. (first markers) -options -icon -options -iconSize)

  @view
  @zoom
  (def shape-layer (get @geometries-map {:coordinates [47.4143390999 8.5498159038],
                                         :name "University of Zurich",
                                         :type :inst-marker,
                                         :id "University of Zurich"}))
  (sort-by :label [{:id 1 :label "b"} {:id 1 :label "c"} {:id 1 :label "a"}])
  (.-lat (.getLatLng shape-layer))
  (.-lat (.getLatLng shape-layer))
  (def icon (.. shape-layer -options -icon))
  (set! (.. icon -options -iconSize) (clj->js [60 60]))
  (.setIcon shape-layer icon)

  (def new-icon (L/Icon.extend (clj->js {:options {:iconSize [400 400]}})))
  (def leaflet @(subscribe [::ll/map]))
  (.removeLayer leaflet shape-layer)
  (.addTo shape-layer leaflet)

  (def inst-icon (.extend L/Icon. (clj->js {:options {:iconAnchor [33 22] #_[20 29]
                                                      :iconSize [400 400]}})))

  (def my-icon (inst-icon. (clj->js {:iconUrl "img/inst-icon.svg"})))

  (def test-marker
    (.addTo
     (L/marker (clj->js [49.7879456863 9.9355841935]))
     leaflet))

  (defn calc-offset [[x y]]
    [(* (/ 33 60) x) (* (/ 22 60) y)])

  (calc-offset [100 100])


  (def new-icon (.extend L/Icon. (clj->js {:options {:iconUrl "img/inst-icon.svg"
                                                     :iconAnchor (calc-offset [100 100]) #_[20 29]
                                                     :iconSize [100 100]}})))

  (.setIcon test-marker (new new-icon))
  (.removeLayer leaflet test-marker)

  (filter #(= (:type %) :inst-marker) @(subscribe [::ll/geometries]))
  (reset! geometries (gen-geometries {:inst? true}))
  (dispatch [::set-leaflet [:geometries] (gen-geometries {:inst? true})])
  (dispatch [::ll/set-leaflet [:geometries]
             [{:type :inst-marker :coordinates [50 50]}
              {:type :marker :coordinates [50 50]}
              {:type :line :coordinates [[50 50] [51 51]]}
              {:type :line :coordinates [[50 51] [50 49]]}
              {:type :line :coordinates [[49 50] [51 50]]}]])
  (dispatch [::set-leaflet [:geometries]
             [#_{:type :point
                 :coordinates [45.7 12.8]}
              {:type :line
               :args {:color :black :weight 1}
               :coordinates [[45.7 12.8]
                             [40.7 10.8]]}]])

  (filter #(= :inst-marker (:type %)) (gen-geometries {:inst? true}))
  (gen-nodes weighted-collab inst-mapping)
  (def weighted-collab (tf/weighted-collab {:inst? true}))
  (apply min (map :weight weighted-collab))
  (first weighted-collab)
  (vec
   (clojure.set/union
    (set (map :node/m weighted-collab))
    (set (map :node/n weighted-collab))))
  (first weighted-collab)
  (def csauthors @(subscribe [::data/csauthors]))
  (first csauthors)
  (def inst? true)
  (def geo-mapping
    (zipmap (map (if inst? :institution :pid) csauthors)
            (map #(hash-map :coord [(:lat %) (:lon %)]
                            :id ((if inst? :institution :pid) %)
                            :name ((if inst? :institution :name) %)) csauthors)))
  (gen-edges weighted-collab geo-mapping)

  (get geo-mapping "University of Münster")
  @(subscribe [::view-position])
  @(subscribe [::zoom-level])
  (count @(subscribe [::ll/geometries]))
  (dispatch [::set-leaflet [:zoom-level] 1])
  (def view-position (atom [49.8 13.1]))
  (def zoom-level (atom 6))
  )
