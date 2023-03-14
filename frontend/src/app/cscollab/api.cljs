(ns app.cscollab.api
  (:require 
   [app.db :as db]
   [app.util :as util]
   [app.components.feedback :as feedback]
   [app.cscollab.common :as common]
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
 ::success-get-data
 (fn [{db :db} [_ id m]]
   (dispatch [::feedback/close id])
   {:db (-> db
            (assoc-in [:data id] m)
            (loaded id))}))

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
 ::get-area-mapping
 (fn [{db :db} _]
   (let [id :get-area-mapping]
     {:db (loading db id)
      :http-xhrio
      {:method          :get
       :uri             (get-api-url "db" "get_area_mapping")
       :response-format (json-response-format {:keywords? true})
       :on-success      [::success-get-data id]
       :on-failure      [::error id]}})))

(reg-event-fx
 ::get-csauthors
 (fn [{db :db} _]
   (let [id :get-csauthors]
     {:db (loading db id)
      :http-xhrio
      {:method          :get
       :uri             (get-api-url "db" "get_csauthors")
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
 ::get-filtered-collab
 (fn [{db :db} [_ config]]
   (let [id :get-filtered-collab]
     (dispatch [::feedback/open :get-filtered-collab])
     {:db (loading db id)
      :http-xhrio
      {:method          :post
       :uri             (get-api-url "db" "get_filtered_collaboration")
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

(reg-event-fx
 ::get-analytics
 (fn [{db :db} [_ config top]]
   (let [id :get-analytics]
     (dispatch [::feedback/open :get-analytics])
     {:db (loading db id)
      :http-xhrio
      {:method          :post
       :uri             (get-api-url "analytics" "get_analytics_collab")
       :params          {"config" (clj->json config),
                         "top" top}
       :format          (json-request-format)
       :response-format (json-response-format {:keywords? true})
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

(reg-sub
 ::map-data-loading?
 :<- [::db/loading? :get-weighted-collab] 
 :<- [::db/loading? :get-frequency]
 (fn [[get-weighted-collab get-frequency]]
   (when (or get-weighted-collab get-frequency)
     true)))

(reg-sub
 ::analytics-data-loading?
 :<- [::db/loading? :get-weighted-collab]
 :<- [::db/loading? :get-filtered-collab]
 :<- [::db/loading? :get-frequency]
 :<- [::db/loading? :get-analytics]
 (fn [[get-weighted-collab get-filtered-collab get-frequency get-analytics]]
   (when (or get-weighted-collab get-filtered-collab get-frequency get-analytics)
     true)))

(reg-event-fx
 ::get-publications-node
 (fn [{db :db} [_ id node config]]
   (let [id-new (keyword (str "get-publications-node" "-" (name id)))]
     (dispatch [::feedback/open id-new])
     {:db (loading db id-new)
      :http-xhrio
      {:method          :post
       :uri             (get-api-url "db" "get_publications_node")
       :params          {"config" (clj->json config),
                         "node" node}
       :format          (json-request-format)
       :response-format (json-response-format {:keywords? true})
       :on-success      [::success-get-data id-new]
       :on-failure      [::error id-new]}})))

(reg-event-fx
 ::get-publications-edge
 (fn [{db :db} [_ id edge config]]
   (let [id-new (keyword (str "get-publications-edge" "-" (name id)))]
     (dispatch [::feedback/open id-new])
     {:db (loading db id-new)
      :http-xhrio
      {:method          :post
       :uri             (get-api-url "db" "get_publications_edge")
       :params          {"config" (clj->json config),
                         "edge" edge}
       :format          (json-request-format)
       :response-format (json-response-format {:keywords? true})
       :on-success      [::success-get-data id-new]
       :on-failure      [::error id-new]}})))

(defn initial-api-call []
  (let [config @(subscribe [::common/filter-config])
        initial-config
        {"from_year" 2015,
         "to_year" 2023,
         "area_ids" ["ai"],
         "sub_area_ids" ["nlp" "ai" "ir" "ml" "vision"],
         "region_ids" ["dach"],
         "country_ids" ["ch" "de" "at"],
         "strict_boundary" true,
         "institution" true}]
    (dispatch [::get-weighted-collab (or config initial-config)])
    (dispatch [::get-region-mapping])
    (dispatch [::get-csauthors])
    (dispatch [::get-area-mapping])
    (dispatch [::get-filtered-collab (or config initial-config)])))

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
  (def config {"from_year" 2015,
               "to_year" 2023,
               "area_ids" ["ai"],
               "sub_area_ids" ["nlp" "ai" "ir" "ml" "vision"],
               "region_ids" ["dach"],
               "country_ids" ["ch" "de" "at"],
               "strict_boundary" true,
               "institution" true})
  (dispatch [::get-analytics config 10])
  @(subscribe [::db/data-field :get-analytics])
  (dispatch [::get-publications-node :graph "EPFL" config])
  (dispatch [::get-publications-edge :graph (clojure.string/split "Graz University of Technology_EPFL" #"_") config])
  @(subscribe [::db/data-field :get-publications-node-graph])
  @(subscribe [::db/data-field :get-publications-edge-graph])
  @(subscribe [::graph-data-loading?])
  (dispatch [::get-node-position config true])
  (dispatch [::get-region-mapping])
  (dispatch [::get-area-mapping])
  (dispatch [::get-csauthors])
  @(subscribe [::db/loading? :get-region-mapping])
  (def app-db re-frame.db/app-db)
  (:errors @app-db)
  (:loading @app-db)
  (get-in @app-db [:loading])
  (dispatch [::get-frequency config])
  (dispatch [::get-weighted-collab config])
  (dispatch [::get-filtered-collab config])
  @(subscribe [::db/data-field :get-filtered-collab])
  @(subscribe [::db/data-field :get-weighted-collab])
  @(subscribe [::db/data-field :get-frequency])
  @(subscribe [::db/data-field :get-region-mapping])
  (first @(subscribe [::db/data-field :get-csauthors]))
  @(subscribe [::db/data-field :get-area-mapping])
  (first @(subscribe [::db/data-field :get-csauthors]))
  (count @(subscribe [::db/data-field :get-weighted-collab]))
  (def get-node-position @(subscribe [::db/data-field :get-node-position]))
  (first (keys get-node-position))
  )