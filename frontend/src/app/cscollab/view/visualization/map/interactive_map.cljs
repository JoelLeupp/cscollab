(ns app.cscollab.view.visualization.map.interactive-map
  (:require [reagent.core :as reagent :refer [atom]]
            [app.cscollab.view.visualization.map.leaflet :as ll :refer [leaflet-map]]
            [app.cscollab.transformer :as tf]
            [app.cscollab.data :as data]
            [app.components.colors :refer [colors]] 
            [app.components.lists :refer [collapse]]
            [app.db :as db] 
            [app.cscollab.common :as common]
            [app.common.container :refer (viz-container)]
            [app.cscollab.view.visualization.selected-info :refer (selected-info-map)] 
            [app.components.feedback :as feedback]
            [app.cscollab.api :as api]
            [app.components.button :as button]
            [reagent-mui.material.paper :refer [paper]]
            [leaflet :as L] 
            [re-frame.core :refer
             (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
            [app.util :as util]))



(defn linear-scale [min-w max-w w]
  "scale for weights between 1 and 2 based on min and max of all weights"
  (let [slope (/ 1 (- max-w min-w))
        shift (- 1 (* min-w slope))]
    (+ shift (* slope w))))

(defn percentil-scale [weights w]
  "scale based on percentil rank between 1 and 2"
  (let [p (util/percentil weights w)
        bin (js/Math.ceil (* 10 p))]
    (condp < bin
          9 2
          8 1.8
          7 1.6
          5 1.4
          1 1.2
          1)))


(defn gen-nodes [weighted-collab geo-mapping insti?]
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
    (vec
     (concat
      (remove
       nil?
       (mapv
        #(let [node-data (get geo-mapping %)]
           (when node-data
             (hash-map :type (if insti? :inst-marker :author)
                       :id (:id node-data)
                       :name (:name node-data)
                       :scale (percentil-scale weights (get node->weight (:id node-data)))
                       #_(linear-scale min-weight max-weight (get node->weight (:id node-data)))
                       :coordinates (:coord node-data))))
        nodes))))))

(defn get-line-weight [w]
  (max 0.2 (min 5 (* 0.1 w))))

(defn gen-edges [weighted-collab geo-mapping insti?]
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

;; get the coordinates along a circle from the origin for a given radius and degree
(defn circle-coord [degree r]
  [(* r (js/Math.sin degree)) (* r (js/Math.cos degree))])

(defn concentrated-circle [nodes]
  (loop [new-nodes []
         nodes-left nodes
         n 0]
    (if (empty? nodes-left)
      new-nodes
      (let [n-nodes (min (count nodes-left) (* 6 (js/Math.pow 2 n)))
            nodes-taken (subvec nodes-left 0 n-nodes)
            radius (* (+ 1 n) 0.005)
            degree (/ (* 2 js/Math.PI) n-nodes)
            node-coord
            (map
             (fn [node t]
               (let [d (+ (* degree t) (when-not (= 0 (mod n 2)) (/ degree 2)))
                     x (* radius (js/Math.sin d))
                     y (* radius (js/Math.cos d))]
                 (merge 
                  node 
                  {:lat-author (+ (/ y 1.5) (:lat node))
                   :lon-author (+ x (:lon node))})))
             nodes-taken (range 0 n-nodes))]
        (recur
         (vec (concat new-nodes node-coord))
         (subvec nodes-left n-nodes)
         (inc n))))))


(defn gen-geometries [{:keys [insti?]}]
  (let [weighted-collab
        @(subscribe [::db/data-field :get-weighted-collab])
        csauthors
        @(subscribe [::data/csauthors])
        geo-mapping-inst
        (zipmap (map :institution csauthors)
                (map #(hash-map :coord [(:lat %) (:lon %)]
                                :id (:institution %)
                                :name (:institution %)) csauthors))
        collab-authors
        (clojure.set/union
         (set (map :node/m weighted-collab))
         (set (map :node/n weighted-collab)))
        csauthors-new-coord ;coordinates of authors in circle around institution
        (flatten
         (for [[_ connected-authors] (group-by 
                                      (juxt :lat :lon) 
                                      (filter #(contains? collab-authors (:pid %)) csauthors))]
           (concentrated-circle connected-authors)))
        geo-mapping-author
        (zipmap (map :pid csauthors-new-coord)
                (map #(hash-map :coord [(:lat-author %) (:lon-author %)]
                                :id (:pid %)
                                :institution (:institution %)
                                :name (:name %)) csauthors-new-coord))
        geo-mapping
        (merge geo-mapping-inst geo-mapping-author)]
    (if (and weighted-collab csauthors geo-mapping)
      (vec
       (concat 
        (gen-edges weighted-collab geo-mapping insti?)
        (gen-nodes weighted-collab geo-mapping insti?)))
      [])))

;; define and view atoms for leaflet componen
(defonce zoom (atom 6))
(defonce view (atom [49.8 13.1]))
(defonce geometries-map (atom {}))

(defn show-node [id]
  (let [coord (js->clj (.. (get @geometries-map id) toGeoJSON -geometry -coordinates))]
    (reset! zoom 13)
    (reset! view [(second coord) (first coord)])))


(defn map-comp [insti?]
  (let [geometries (subscribe [::ll/geometries])] 
    (fn []
      (when (empty? @geometries)
        (dispatch [::ll/set-leaflet [:geometries] (gen-geometries {:insti? insti?})]))
      [leaflet-map
       {:id "interactive-map"
        :zoom zoom
        :view view
        :geometries-map geometries-map
        :layers
        [{:type :tile
          :url "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
          :attribution "&copy; <a href=\"http://www.openstreetmap.org/copyright\">OpenStreetMap</a>"}]
        :style {:width "100%" :height "100%" :z-index 1}}])))



(defn update-data []
  (let [config @(subscribe [::common/filter-config])]
    (dispatch [::api/get-weighted-collab config])
    (dispatch [::api/get-frequency config])
    (dispatch [::api/get-filtered-collab config]) 
    (dispatch [::api/get-analytics config 200])))

(defonce reset (atom 0))

(defn update-event [] 
  (swap! reset inc)
  (dispatch [::ll/set-leaflet [:info-open?] false])
  (dispatch [::ll/set-leaflet [:selected-shape] nil])
  (update-data))

(defn interactive-map []
  (let [insti? (subscribe [::data/insti?])
        loading? (subscribe [::api/map-data-loading?])
        color-by (subscribe [::data/color-by])]
    (add-watch loading? ::map-data-loading
               (fn [_ _ _ data-loading?]
                 (when (= :map @(subscribe [::db/ui-states-field [:tabs :viz-view]]))
                   (if data-loading?
                     (dispatch [::feedback/open :map-data-loading])
                     (do
                       (dispatch [::feedback/close :map-data-loading])
                       (dispatch [::ll/set-leaflet [:geometries] (gen-geometries {:insti? @(subscribe [::data/insti?])})]))))))
    (fn [] 
      ^{:key [@loading? @reset]}
      [:div
       [feedback/feedback {:id :map-data-loading
                           :anchor-origin {:vertical :top :horizontal :center}
                           :status :info
                           :auto-hide-duration nil
                           :message "Garaph data is loading, please wait."}]
       [viz-container
        {:id :map-container
         :legend-bg-color :white
         :color-by @color-by
         :title "Landscape of Scientific Collaborations" 
         :update-event #(update-event) #_(dispatch [::ll/set-leaflet [:geometries] (gen-geometries {:insti? @insti?})])
         :content [map-comp @insti?] 
         :info-component [selected-info-map]
         :info-open? (subscribe [::ll/info-open?])}]])))


