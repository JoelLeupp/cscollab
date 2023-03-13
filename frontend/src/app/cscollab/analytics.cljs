(ns app.cscollab.analytics
  (:require [reagent.core :as reagent :refer [atom]] 
            [app.cscollab.data :as data]
            [app.components.colors :refer [colors]]
            [app.components.lists :refer [collapse]]
            [app.db :as db]
            [app.common.container :refer (analytics-container)]
            [app.cscollab.common :as common] 
            [app.cscollab.map-panel :as mp]
            [app.components.feedback :as feedback]
            [app.cscollab.api :as api]
            [app.components.button :as button] 
            [re-frame.core :refer
             (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
            [app.util :as util]))


(defn get-analysis-graph-data []
  (let [top 20 
        config @(subscribe [::common/filter-config])
        color-by @(subscribe [::mp/color-by])
        sub-areas? (if (= color-by :subarea) true false)]
    (dispatch [::api/get-analytics config 10])
    (dispatch [::api/get-weighted-collab config])
    (dispatch [::api/get-frequency config])
    (dispatch [::api/get-filtered-collab config])))


(defn analytics-view []
  (let [loading? (subscribe [::api/analytics-data-loading?])
        analytics (subscribe [::db/data-field :get-analytics])]
    (add-watch
     loading? ::analytics-data-loading
     (fn [_ _ _ data-loading?]
       (if data-loading?
         (dispatch [::feedback/open :analytics-data-loading])
         (dispatch [::feedback/close :analytics-data-loading]))))
    (fn []
      [analytics-container
       {:title "Collaboration Graph Analytics and Statistics"
        :content [:span @analytics] #_[:div {:style {:margin 0 :padding 0 :width "100%" :height "100%" :text-align :center}}]
        :update-event #(get-analysis-graph-data)}])))

