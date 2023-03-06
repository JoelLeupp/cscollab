(ns app.cscollab.api
  (:require 
   [app.db :as db]
   [app.util :as util]
   [app.components.feedback :as feedback]
   [re-frame.core :refer
    (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
   [ajax.core :as ajax :refer (json-request-format json-response-format)]))

(defn get-api-url [tag api]
  (str "https://cscollab.ifi.uzh.ch/backend/api/" 
       #_"http://127.0.0.1:8030/api/"
       tag "/" api))

(defn clj->json
  [ds]
  (.stringify js/JSON (clj->js ds)))

(defn loading [db id]
  (assoc-in db [:loading id] true))

(defn loaded [db id]
  (assoc-in db [:loading id] false))


(reg-event-fx
 ::error
 (fn [{db :db} [_ id m]]
   (dispatch [::feedback/close id])
   {:db (-> db
            (update :errors #(conj % m))
            (loaded id))}))

(reg-event-fx
 ::get-region-mapping
 (fn [{db :db} _]
   (let [id :get-region-mapping]
     {:db (loading db id)
      :http-xhrio
      {:method          :get
       :uri             (get-api-url "db" "get_region_mapping")
       :response-format (json-response-format {:keywords? true})
       :on-success      [::success-get-data id]
       :on-failure      [::error id]}})))

(reg-event-fx
 ::get-weighted-collab
 (fn [{db :db} [_ config]]
   (let [id :get-weighted-collab]
     (dispatch [::feedback/open :get-weighted-collab])
     {:db (loading db id)
      :http-xhrio
      {:method          :post
       :uri             (get-api-url "db" "get_weighted_collab")
       :params          {"config" (clj->json config)}
       :format          (json-request-format)
       :response-format (json-response-format {:keywords? true})
       :on-success      [::success-get-data id]
       :on-failure      [::error id]}})))


(reg-event-fx
 ::get-node-position
 (fn [{db :db} [_ config sub-areas?]]
   (let [id :get-node-position]
     (dispatch [::feedback/open :get-node-position])
     {:db (loading db id)
      :http-xhrio
      {:method          :post
       :uri             (get-api-url "gcn" "get_node_position")
       :params          {"config" (clj->json config),
                         "sub_areas" sub-areas?}
       :format          (json-request-format)
       :response-format (json-response-format)
       :on-success      [::success-get-data id]
       :on-failure      [::error id]}})))

(reg-event-fx
 ::get-frequency
 (fn [{db :db} [_ config]]
   (let [id :get-frequency]
     (dispatch [::feedback/open :get-frequency])
     {:db (loading db id)
      :http-xhrio
      {:method          :post
       :uri             (get-api-url "db" "get_frequency_research_field")
       :params          {"config" (clj->json config)}
       :format          (json-request-format)
       :response-format (json-response-format)
       :on-success      [::success-get-data id]
       :on-failure      [::error id]}})))


(reg-sub
 ::graph-data-loading?
 :<- [::db/loading? :get-weighted-collab]
 :<- [::db/loading? :get-node-position]
 :<- [::db/loading? :get-frequency]
 (fn [[get-weighted-collab get-node-position get-frequency]]
   (when (or get-weighted-collab get-node-position get-frequency)
     true)))

(reg-event-fx
 ::success-get-data
 (fn [{db :db} [_ id m]] 
   (dispatch [::feedback/close id])
   {:db (-> db
            (assoc-in [:data id] m)
            (loaded id))}))

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
  (def config {"from_year" 2015
               "region_ids" ["dach"]
               "strict_boundary" true,
               "institution" true})
  @(subscribe [::graph-data-loading?])
  (dispatch [::get-node-position config true]) 
  (dispatch [::get-region-mapping])
  @(subscribe [::db/loading? :get-region-mapping])
  (def app-db re-frame.db/app-db)
  (:errors @app-db)
  (:loading @app-db)
  (get-in @app-db [:loading ])
  (dispatch [::get-frequency config])
  (dispatch [::get-weighted-collab config])
  @(subscribe [::db/data-field :get-region-mapping])
  @(subscribe [::db/data-field :get-frequency])
  (count @(subscribe [::db/data-field :get-weighted-collab]))
  (def get-node-position @(subscribe [::db/data-field :get-node-position]))
  (first (keys get-node-position)) 
  )