(ns app.cscollab.interactive-map
  (:require [reagent.core :as reagent :refer [atom]]
            [app.common.leaflet :as ll :refer [leaflet-map]]
            [app.cscollab.transformer :as tf]
            [app.cscollab.data :as data]
            [app.components.lists :refer [collapse]]
            [app.db :as db]
            [app.components.button :as button]
            [reagent-mui.material.paper :refer [paper]]
            [leaflet :as L]
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
                       (hash-map :type :point
                                 :radius (/ 111320 100) 
                                 :weight (get-line-weight (:weight %))
                                 :id [(:id node-m) (:id node-n)]
                                 :coordinates 
                                 [(- (first (:coord node-m)) (/ 1 100))
                                  (second (:coord node-m))])
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
(defonce geometries-map (atom nil))

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


(defn inst-info []
  (let [selected-shape (subscribe [::ll/selected-shape])
        filtered-collab (subscribe [::tf/filtered-collab])]
    (fn []
      (when (and @selected-shape @filtered-collab)
        (let [institution @selected-shape 
              inst-collab
              (filter
               #(or (= institution (:a_inst %))
                    (= institution (:b_inst %)))
               @filtered-collab)
              collab-count
              (count (set (map :rec_id inst-collab)))
              author-count
              (count
               (vec
                (set
                 (flatten
                  (map
                   #(concat
                     []
                     (when (= institution (:a_inst %))
                       [(:a_pid %)])
                     (when (= institution (:b_inst %))
                       [(:b_pid %)]))
                   inst-collab)))))]
          [:div
           [:h4 institution]
           [:span (str "Number of authors: " author-count)]
           [:br]
           [:span (str "Number of collaborations: " collab-count)]])))))

(def test-a (atom nil))
(first @test-a)

(defn collab-info []
  (let [selected-shape (subscribe [::ll/selected-shape])
        filtered-collab (subscribe [::tf/filtered-collab])]
    (fn []
      (when (and (vector? @selected-shape) @filtered-collab) 
        (let [selected-collab @selected-shape
              records
              (filter
               #(or
                 (and (= (first selected-collab) (:b_inst %))
                      (= (second selected-collab) (:a_inst %)))
                 (and (= (first selected-collab) (:a_inst %))
                      (= (second selected-collab) (:b_inst %))))
               @filtered-collab)
              number-collabs (count (set (map :rec_id records)))]
          (reset! test-a records)
          [:div
           [:h4  "Collaboration"]
           [:h4 
            (first selected-collab) " And " (second selected-collab)]
           [:span (str "Number of collaborations: " number-collabs)]])))))

(defn map-info [{:keys [inst?]}]
  (let [selected-shape (subscribe [::ll/selected-shape])]
    (fn []
      (if (string? @selected-shape)
        [inst-info] 
        [collab-info]))))

(defn map-info-div [] 
  (fn []
    [collapse
     {:sub (subscribe [::ll/info-open?])
      :div
      [:div {:style {:position :absolute :right "10%" :z-index 10}}
       [:div {:style {:background-color :white :height "70vh" :min-width "350px" :padding-left 10 :padding-right 10}}
        [:div {:style {:display :flex :justify-content :space-between}}
         [:h3 "Info Selected"]
         [button/close-button
          {:on-click #(dispatch [::ll/set-leaflet [:info-open?] false])}]]
        [map-info {inst? true}]]]}]))

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
  (def selected-collab @(subscribe [::ll/selected-shape]))
  (dispatch [::ll/set-leaflet [:info-open?] true])
  (def filtered-collab @(subscribe [::tf/filtered-collab]))
  
  (let [collabs
        (filter
         #(or
           (and (= (first selected-collab) (:b_inst %))
                (= (second selected-collab) (:a_inst %)))
           (and (= (first selected-collab) (:a_inst %))
                (= (second selected-collab) (:b_inst %))))
         filtered-collab)
        number-collabs (count (set (map :rec_id collabs)))]
    [:div
     [:h4 (str "Collaboration Between\n"
                 (first selected-collab)
                 "\nAnd\n"
                 (second selected-collab))]
     [:span (str "Number of collaborations: " number-collabs)]])
  
  (first filtered-collab)
  
  (def collab
    (filter
     #(or
       (and (= (first selected-collab) (:b_inst %))
            (= (second selected-collab) (:a_inst %)))
       (and (= (first selected-collab) (:a_inst %))
            (= (second selected-collab) (:b_inst %))))
     filtered-collab))
  (count inst-collab)
  (count
   (vec
    (set
     (flatten
      (map
       #(concat
         []
         (when (= "Max Planck Society" (:a_inst %))
           [(:a_pid %)])
         (when (= "Max Planck Society" (:b_inst %))
           [(:b_pid %)]))
       inst-collab)))))
  
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
     (L/marker (clj->js [47.4143390999 8.5498159038]) #js {:icon my-icon})
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
  (def geometries
    (atom [#_{:type :line
              :coordinates [[45.7 12.8]
                            [40.7 10.8]]}
           #_{:type :marker
              :coordinates [45.7 12.8]}]))
  "University of OsnabrÃ¼ck"
  )
