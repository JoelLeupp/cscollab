(ns app.cscollab.interactive-map
  (:require [reagent.core :as reagent :refer [atom]]
            [app.common.leaflet :as ll :refer [leaflet-map]]
            [app.cscollab.transformer :as tf]
            [app.cscollab.data :as data]
            [app.db :as db]
            [app.components.button :as button]
            [reagent-mui.material.paper :refer [paper]]
            [re-frame.core :refer
             (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]))



(defn gen-nodes [weighted-collab geo-mapping]
  (let [nodes (vec (clojure.set/union
                    (set (map :node/m weighted-collab))
                    (set (map :node/n weighted-collab))))]
    ;; temorary remove nil because of utf8 string conflict
    (vec (remove nil? 
                 (mapv
                  #(let [node-data (get geo-mapping %)]
                     (when node-data
                       (hash-map :type :inst-marker
                                 :id (:id node-data)
                                 :name (:name node-data)
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
                       (hash-map :type :line
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

(defn interactive-map []  
  (let [geometries (subscribe [::ll/geometries])]
    (fn []  
      (when (empty? @geometries)
        (dispatch [::ll/set-leaflet [:geometries] (gen-geometries {:inst? true})]))
      [paper {:elevation 1}
       [:<>
        [:div {:style {:display :flex 
                       :justify-content
                       :space-between 
                       :background-color :white
                       :paddin-top 0
                       :padding-left 20
                       :padding-right 20}}
         [:h1 {:style {:margin 10}} "Landscape of Scientific Collaborations"]
         [button/update-button
          {:on-click #(dispatch [::ll/set-leaflet [:geometries] (gen-geometries {:inst? true})])
           :style {:z-index 999}}]] 
        [leaflet-map
         {:id "interactive-map"
          :zoom zoom
          :view view
          :layers
          [{:type :tile
            :url "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
            :attribution "&copy; <a href=\"http://www.openstreetmap.org/copyright\">OpenStreetMap</a>"}]
          :style {:width "100%" :height "70vh"} 
          }]]])))

(comment
  @view
  @zoom
  (reset! geometries (gen-geometries {:inst? true}))
  (dispatch [::set-leaflet [:geometries] (gen-geometries {:inst? true})])
  (dispatch [::ll/set-leaflet [:geometries] 
             [{:type :inst-marker :coordinates [50 50]}
              {:type :marker :coordinates [50 50]}
              {:type :line :coordinates [[49 49] [51 51]]}
              ]])
  (dispatch [::set-leaflet [:geometries]
             [#_{:type :point
               :coordinates [45.7 12.8]}
              {:type :line
               :args {:color :black :weight 1}
               :coordinates [[45.7 12.8]
                             [40.7 10.8]]}]])

  (gen-geometries {:inst? true})
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
  (count @(subscribe [::geometries]))
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
