(ns app.cscollab.interactive-map
  (:require [reagent.core :as reagent :refer [atom]]
            [app.common.leaflet :refer [leaflet]]
            [app.cscollab.transformer :as tf]
            [app.cscollab.data :as data]
            [app.db :as db]
            [app.components.button :as button]
            [re-frame.core :refer
             (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]))


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
  :<-[::leaflet-field :view-position]
  (fn [p] (when p p)))

(reg-sub
 ::zoom-level
 :<- [::leaflet-field :zoom-level]
 (fn [z] (when z z)))

(reg-sub
 ::geometries
 :<- [::leaflet-field :geometries]
 (fn [g] (when g g)))



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
    (gen-nodes weighted-collab geo-mapping)))


(defn interactive-map [] 
  (let [view-position (subscribe [::view-position])
        zoom-level (subscribe [::zoom-level])
        geometries (subscribe [::geometries])
        leaflet-data (subscribe [::leaflet])]
    (fn []  
      [:<>
       [:div {:style {:display :flex :justify-content :center}}
        [button/update-button
         {:on-click #(dispatch [::set-leaflet [:geometries] (gen-geometries {:inst? true})])
          :style {:padding 20 :z-index 999 }}]]
       ^{:key @leaflet-data}
       [leaflet
        {:id "interactive-map"
         :layers
         [{:type :tile
           :url "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
           :attribution "&copy; <a href=\"http://www.openstreetmap.org/copyright\">OpenStreetMap</a>"}]
         :style {:width "100%" :height "800px"} 
         :view view-position ;; map center position
         :zoom zoom-level ;; map zoom level
         :geometries geometries ;; Geometry shapes to draw to the map
         }]])))

(comment
  (dispatch [::set-leaflet [:geometries] (gen-geometries {:inst? true})])
  (gen-geometries {:inst? true})
  (gen-nodes weighted-collab inst-mapping)
  (def weighted-collab (tf/weighted-collab {:inst? true}))
  (vec
   (clojure.set/union
    (set (map :node/m weighted-collab))
    (set (map :node/n weighted-collab))))
  (first weighted-collab)
  (def csauthors @(subscribe [::data/csauthors]))
  (first csauthors)
  (def inst-coord
    (zipmap (map :institution csauthors) (map #(hash-map :lat (:lat %) :lon (:lon %)) csauthors)))
  (get geo-mapping "University of Münster")
  @(subscribe [::view-position])
  @(subscribe [::zoom-level])
  @(subscribe [::leaflet])
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
