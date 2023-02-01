(ns app.cscollab.data
  (:require
   [datascript.core :as d]
   [app.db :as db]
   [re-frame.core :refer
    (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)] 
   [ajax.core :as ajax :refer (json-request-format json-response-format)]))


(reg-event-fx
 ::get-json-file
 (fn [{db :db} [_ json-path id]]
   {:db db
    :http-xhrio
    {:method          :get
     :uri             json-path
     :format          (json-request-format)
     :response-format (json-response-format {:keywords? true})
     :on-success      [::success-get-json id]
     :on-failure      [::success-get-json id]}}))

(reg-event-fx
 ::success-get-json
 (fn [{db :db} [_ id m]]
   {:db (assoc-in db [:data id] m)}))

(defn get-json-data [] 
  (dispatch [::get-json-file "data/get_area_mapping.json" :area-mapping])
  (dispatch [::get-json-file "data/get_csauthors.json" :csauthors])
  (dispatch [::get-json-file "data/get_flat_collaboration.json" :collab])
  (dispatch [::get-json-file "data/get_region_mapping.json" :region-mapping]))

(reg-sub
 ::area-mapping
 :<- [::db/data-field [:area-mapping]]
 (fn [m] 
   (when m m)))

(reg-sub
 ::csauthors
 :<- [::db/data-field [:csauthors]]
 (fn [m]
   (when m m)))

(reg-sub
 ::collab
 :<- [::db/data-field [:collab]]
 (fn [m]
   (when m m)))

(reg-sub
 ::region-mapping
 :<- [::db/data-field [:region-mapping]]
 (fn [m]
   (when m m)))

(reg-sub
 ::nested-area
 :<- [::area-mapping]
 (fn
   [area-mapping]
   "generates a nested structure out of the flat area-mapping table like:
    [{:id * 
      :label * 
      :sub-areas [{:id *
                   :label * 
                   :conferences [{:id * :label *} ...]} 
                 ...]} ...]"
   (when area-mapping
     (mapv
      #(into
        {}
        [{:id
          (-> % first first)
          :label
          (-> % first second)}
         {:sub-areas
          (mapv
           (fn [[k v]]
             (into
              {}
              [{:id (first k) :label (second k)}
               {:conferences
                (mapv (fn [x] (hash-map
                               :id (:conference-id x)
                               :label (:conference-title x)))
                      v)}]))
           (group-by (juxt :sub-area-id :sub-area-label) (second %)))}])
      (group-by (juxt :area-id :area-label) area-mapping)))))



(comment
  (get-json-data)
  (def area-mapping @(subscribe [::db/data-field [:area-mapping]]))
  (def group-area (group-by (juxt :area-id :area-label) area-mapping))
  (map #(-> % first) group-area)
  @(subscribe [::nested-area-t])
  (map :id area-dict)
  (first area-mapping)
  @(subscribe [::db/data-field [:csauthors]])
  (def collab @(subscribe [::db/data-field [:collab]]))
  (first collab)
  @(subscribe [::db/data-field [:region_mapping]])
  (into {} [{:id 1} {:a [1 2 3]}])
  )
