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
  (dispatch [::get-json-file "data/get_region_mapping.json" :region_mapping]))

(comment
  (get-json-data)
  @(subscribe [::db/data-field [:area-mapping]])
  @(subscribe [::db/data-field [:csauthors]])
  (def collab @(subscribe [::db/data-field [:collab]]))
  (first collab)
  @(subscribe [::db/data-field [:region_mapping]])
  )
