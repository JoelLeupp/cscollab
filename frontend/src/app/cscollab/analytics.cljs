(ns app.cscollab.analytics
  (:require [reagent.core :as reagent :refer [atom]]
            [app.cscollab.data :as data]
            [app.components.colors :refer [colors]]
            [app.components.lists :refer [collapse]]
            [app.db :as db]
            [app.components.table :as table]
            [app.common.container :refer (analytics-container)]
            [app.cscollab.common :as common]
            [app.cscollab.map-panel :as mp]
            [app.components.feedback :as feedback]
            [app.cscollab.api :as api]
            [app.components.button :as button]
            [goog.string :as gstring]
            [goog.string.format]
            [re-frame.core :refer
             (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
            [app.util :as util]))


(defn get-analytics-graph-data []
  (let [top 20
        config @(subscribe [::common/filter-config])]
    (dispatch [::api/get-analytics config top])
    (dispatch [::api/get-weighted-collab config])
    (dispatch [::api/get-frequency config])
    (dispatch [::api/get-filtered-collab config])))


(defn analytics-test []
  (let [analytics (subscribe [::db/data-field :get-analytics])]
    (fn []
      )))

(defn statistics-table []
  (let [analytics (subscribe [::db/data-field :get-analytics])
        collab (subscribe [::db/data-field :get-filtered-collab])
        insti? (subscribe [::mp/insti?])]
    (fn []
      (let [statistics (get @analytics :statistics)
            n-publications (count (set (map :rec_id @collab)))
            header [{:id :metric :label [:b "Statistics"]}
                    {:id :value :label "" :align :right}]]
        (when @analytics
          [table/basic-table
           {:header header
            :body [{:metric (str "Size (Number of " (if @insti? "Institutions" "Authors") ")") :value (:nodes statistics)}
                   {:metric "Volume (Number of Collaborations)" :value (:edges statistics)}
                   {:metric "Number of Publications" :value n-publications}
                   {:metric "Number of Triangles" :value (:triangle_count statistics)}
                   {:metric "Average Degree" :value (gstring/format "%.1f" (:average_degree statistics))}
                   {:metric "Max Degree" :value (:max_degree statistics)}
                   {:metric "Degree Assortativity Coefficient" :value (gstring/format "%.3f" (:degree_assortativity_coefficient statistics))}
                   {:metric "Density" :value (gstring/format "%.3f" (:density statistics))}
                   {:metric "Graph is connected?" :value (if (:is_connected statistics) "yes" "no")}
                   {:metric "Number Connected Components" :value (:number_connected_components statistics)}
                   {:metric "Largest Connected Component" :value (:largest_connected_component statistics)}]
            :paper-args {:sx {:width 400 :margin 5}}
            :container-args {:sx {}}
            :table-args {:sticky-header true :size :small}}])))))

(defn analytics-view []
  (let [loading? (subscribe [::api/analytics-data-loading?])
        analytics (subscribe [::db/data-field :get-analytics])]
    (get-analytics-graph-data)
    (add-watch
     loading? ::analytics-data-loading
     (fn [_ _ _ data-loading?]
       (if data-loading?
         (dispatch [::feedback/open :analytics-data-loading])
         (dispatch [::feedback/close :analytics-data-loading]))))
    (fn []
      ^{:key [@loading?]}
      [:div
       [feedback/feedback {:id :analytics-data-loading
                           :anchor-origin {:vertical :top :horizontal :center}
                           :status :info
                           :auto-hide-duration nil
                           :message "Analytics is loading, please wait."}]
       [analytics-container
        {:title "Analytics and Statistics"
         :content [statistics-table] #_[:div {:style {:margin 0 :padding 0 :width "100%" :height "100%" :text-align :center}}]
         :update-event #(get-analytics-graph-data)}]])))

(comment 
  (get-analytics-graph-data)
  (def analytics @(subscribe [::db/data-field :get-analytics]))
  (get analytics :statistics)
  (def collab @(subscribe [::db/data-field :get-filtered-collab]))
  (count (set (map :rec_id collab)))
  )
