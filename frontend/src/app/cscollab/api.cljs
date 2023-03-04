(ns app.cscollab.api
  (:require 
   [app.db :as db]
   [app.util :as util]
   [re-frame.core :refer
    (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
   [ajax.core :as ajax :refer (json-request-format json-response-format)]))

(defn get-api-url [tag api]
  (str "https://cscollab.ifi.uzh.ch/backend/api/" tag "/" api))

(reg-event-fx
 ::error
 (fn [{db :db} [_ m]]
   (print "error")
   {:db (update db :errors #(conj % m) )}))

(defn loading [db id]
  (assoc-in db [:loading id] true))

(defn loaded [db id]
  (assoc-in db [:loading id] false))

(reg-event-fx
 ::get-region-mapping
 (fn [{db :db} _]
   (let [id :get-region-mapping]
     {:db (loading db id)
      :http-xhrio
      {:method          :get
       :uri             (get-api-url "db" "get_region_mapping")
       :format          (json-request-format)
       :response-format (json-response-format {:keywords? true})
       :on-success      [::success-get-data id]
       :on-failure      [::error]}})))


(reg-event-fx
 ::success-get-data
 (fn [{db :db} [_ id m]]
   (print "sucess")
   {:db (assoc-in db [:data id] m) 
    #_(-> db 
           (assoc-in [:data id] m)
           (assoc-in [:loading id] true))}))

#_(reg-event-fx
 ::http-post
 (fn [_world [_ val]]
   {:http-xhrio {:method          :post
                 :uri             "https://httpbin.org/post"
                 :params          data
                 :timeout         5000
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [::success-post-result]
                 :on-failure      [::failure-post-result]}}))

(comment 
  (get-api-url "db" "get_region_mapping")
  (dispatch [::get-region-mapping])
  (def app-db re-frame.db/app-db)
  (:errors @app-db)
  (:loading @app-db)
  (keys (:data @app-db))
  @(subscribe [::db/data-field :get-region-mapping])
  )