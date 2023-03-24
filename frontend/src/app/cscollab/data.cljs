(ns app.cscollab.data
  (:require
   [datascript.core :as d]
   [app.db :as db]
   [app.util :as util]
   [re-frame.core :refer
    (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
   [ajax.core :as ajax :refer (json-request-format json-response-format)]))


#_(reg-event-fx
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

#_(reg-event-fx
 ::success-get-json
 (fn [{db :db} [_ id m]]
   {:db (assoc-in db [:data id] m)}))

#_(defn get-json-data []
  (dispatch [::get-json-file "data/get_area_mapping.json" :area-mapping])
  (dispatch [::get-json-file "data/get_csauthors.json" :csauthors])
  (dispatch [::get-json-file "data/get_flat_collaboration.json" :collab])
  (dispatch [::get-json-file "data/get_region_mapping.json" :region-mapping]))

(reg-sub
 ::insti?
 :<- [::db/user-input-field [:insti?]]
 (fn [insti?]
   insti?))

(reg-sub
 ::color-by
 :<- [::db/user-input-field [:color-by]]
 (fn [color-by]
   (when (and color-by (not (= color-by :no-coloring)))
     color-by)))

(reg-sub
 ::area-mapping
 :<- [::db/data-field :get-area-mapping]
 (fn [m]
   (when m m)))

(reg-sub
 ::csauthors
 :<- [::db/data-field :get-csauthors]
 (fn [m]
   (when m m)))


(reg-sub
 ::region-mapping
 :<- [::db/data-field :get-region-mapping]
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
          (util/s->id (-> % first first))
          :label
          (-> % first second)}
         {:sub-areas
          (mapv
           (fn [[k v]]
             (into
              {}
              [{:id (util/s->id (first k)) :label (second k)}
               {:conferences
                (sort-by
                 :label
                 (mapv (fn [x] (hash-map
                                :id (util/s->id (:conference-id x))
                                :label (:conference-title x)))
                       v))}]))
           (group-by (juxt :sub-area-id :sub-area-label) (second %)))}])
      (group-by (juxt :area-id :area-label) area-mapping)))))


(reg-sub
 ::nested-region
 :<- [::region-mapping]
 (fn
   [region-mapping]
   "generates a nested structure out of the flat region-mapping table like:
    [{:id * 
      :label * 
      :countries [{:id *
                   :label *} 
                 ...]} ...]"
   (when region-mapping
     (mapv
      #(into
        {}
        [{:id
          (util/s->id (-> % first first))
          :label
          (-> % first second)}
         {:countries
          (mapv
           (fn [[k _]]
             (hash-map :id (util/s->id (first k)) :label (second k)))
           (group-by (juxt :country-id :country-name) (second %)))}])
      (group-by (juxt :region-id :region-name) region-mapping)))))



