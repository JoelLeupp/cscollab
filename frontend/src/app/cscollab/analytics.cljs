(ns app.cscollab.analytics
  (:require [reagent.core :as reagent :refer [atom]]
            [app.cscollab.data :as data]
            [app.components.colors :refer [colors]]
            [app.components.lists :refer [collapse]]
            [app.db :as db]
            [app.components.grid :as grid]
            [app.components.table :as table]
            [app.common.container :refer (analytics-container)]
            [app.components.stack :refer (horizontal-stack)]
            [app.cscollab.common :as common]
            [app.cscollab.map-panel :as mp]
            [app.components.feedback :as feedback]
            [app.cscollab.api :as api]
            [app.components.tabs :as tabs]
            [app.common.plotly :as plotly]
            [app.components.lists :as lists]
            [app.components.button :as button]
            [goog.string :as gstring]
            [app.components.inputs :as i]
            [goog.string.format]
            [re-frame.core :refer
             (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
            [app.util :as util]))


(defn get-analytics-graph-data []
  (let [top 200
        config @(subscribe [::common/filter-config])]
    (dispatch [::api/get-analytics config top])
    (dispatch [::api/get-weighted-collab config])
    (dispatch [::api/get-frequency config])
    (dispatch [::api/get-filtered-collab config])))



(defn statistics-table []
  (let [analytics (subscribe [::db/data-field :get-analytics])
        collab (subscribe [::db/data-field :get-filtered-collab])
        insti? (subscribe [::mp/insti?])]
    (fn []
      (let [statistics (get @analytics :statistics)
            n-publications (count (set (map :rec_id @collab)))
            weighted-edges (count @collab)
            header [{:id :metric :label [:b "Filtered Graph Statistics"] }
                    {:id :value :label "" :align :right}]]
        (when @analytics
          [table/basic-table
           {:header header
            :body [{:metric (str "Number of " (if @insti? "Institutions" "Authors")) :value (:nodes statistics)}
                   {:metric "Number of Edges" :value (:edges statistics)}
                   {:metric "Number of Collaborations" :value weighted-edges}
                   {:metric "Number of Publications" :value n-publications}
                   {:metric "Number of Triangles" :value (:triangle_count statistics)}
                   {:metric "Average Degree" :value (gstring/format "%.1f" (:average_degree statistics))}
                   {:metric "Max Degree" :value (:max_degree statistics)} 
                   {:metric "Graph Density" :value (gstring/format "%.3f" (:density statistics))}
                   {:metric "Graph is connected?" :value (if (:is_connected statistics) "yes" "no")}
                   {:metric "Number Connected Components" :value (:number_connected_components statistics)}
                   {:metric "Largest Connected Component" :value (:largest_connected_component statistics)}]
            :paper-args {:sx {:width 400}}
            :container-args {:sx {}}
            :table-args {:sticky-header true :size "small"}}])))))

(defn get-plot-data [x y]
  (identity
   [{:x x
     :y y 
     :type :bar
     :orientation :h
     :hoverinfo "none"
     :textposition :outside
     :text (mapv #(gstring/format "%.2f" (str %)) x)
     :transforms [{:type :sort :target :x :order :descending}]
     :marker {:color (:main colors)}}]))

(defn hbar-plot [plot-data layout]
  (fn []
    [plotly/plot
     {:box-args {:height "60vh"  :width "35vw" :overflow :auto :margin 0}
      :style {:width "33vw" :height (max 300 (+ 150 (* 45 (count (:x (first plot-data))))))}
      :layout (util/deep-merge
               {:margin  {:pad 10 :t 0 :b 30 :l 200 :r 10}
                :bargap 0.2
                :showlegend false
                :xaxis {}
                :yaxis {:autorange :reversed
                        :tickmode :array}}
               layout)
      :data plot-data}]))

(defn centrality-plot [centrality-key]
  (let [analytics (subscribe [::db/data-field :get-analytics])
        top (subscribe [::db/user-input-field :top-centrality])]
    (fn []
      (let [centrality-data (get-in @analytics [:centralities centrality-key])
            top-data (subvec centrality-data 0 (min (count centrality-data) @top))
            x (mapv :value top-data)
            y (mapv :id top-data)
            data (get-plot-data x y)]
        ^{:key @top}
        [hbar-plot data {:xaxis {:range [0 (+ 0.1 (apply max x))]}}]))))

(defn top-centrality []
  [i/select
   {:id :top-centrality
    :label nil
    :form-args {:variant :standard
                :style {:min-width nil :height "100%" :display :flex :align-self :flex-end :margin-bottom 10}}
    :choices [{:value 5 :label 5}
              {:value 10 :label 10}
              {:value 20 :label 20}
              {:value 50 :label 50}
              {:value 100 :label 100}
              {:value 200 :label 200}]}])

(defn centraliy-div []
  (fn []
    [grid/grid
     {:grid-args {:justify-content :space-evenly}
      :item-args {:elevation 0}
      :content
      [{:xs 6
        :content
        [:div
         [horizontal-stack
          {:stack-args {:spacing 0}
           :items
           (list
            [lists/sub-header {:subheader "Degree Centrality Top"}]
            [top-centrality]
            )}]
         [centrality-plot :degree_centrality]]}
       {:xs 6
        :content
        [:div
         [horizontal-stack
          {:stack-args {:spacing 0}
           :items
           (list
            [lists/sub-header {:subheader "Eigenvector Centrality Top"}]
            [top-centrality])}] 
         [centrality-plot :eigenvector_centrality]]}]}]))

(defn analytics-content []
  (let [tab-view (subscribe [::db/ui-states-field [:tabs :analytics-tabs]])]
    (fn [] 
      [:div {:style {:margin-left 30}} 
       (case @tab-view
         :statistics [statistics-table]
         :centralities [centraliy-div]
         [statistics-table])])))

(defn analytics-view []
  (let [loading? (subscribe [::api/analytics-data-loading?])
        analytics (subscribe [::db/data-field :get-analytics])]
    (get-analytics-graph-data)
    (add-watch
     loading? ::analytics-data-loading
     (fn [_ _ _ data-loading?]
       (when (= :analytics @(subscribe [::db/ui-states-field [:tabs :viz-view]]))
         (if data-loading?
           (dispatch [::feedback/open :analytics-data-loading])
           (dispatch [::feedback/close :analytics-data-loading])))))
    (fn []
      ^{:key [@loading? @analytics]}
      [:div
       [feedback/feedback {:id :analytics-data-loading
                           :anchor-origin {:vertical :top :horizontal :center}
                           :status :info
                           :auto-hide-duration nil
                           :message "Analytics is loading, please wait."}]
       [analytics-container
        {:title [tabs/sub-tab
                 {:id :analytics-tabs
                  :tabs-args {:variant :scrollable :scrollButtons :auto}
                  :box-args {:margin-bottom 0 :border-bottom 0 :width 460}
                  :choices
                  [{:label "Statistics" :value :statistics}
                   {:label "Centralities" :value :centralities}]}]
         :content [analytics-content] #_[:div {:style {:margin 0 :padding 0 :width "100%" :height "100%" :text-align :center}}]
         :update-event #(get-analytics-graph-data)}]])))

(comment 
  (get-analytics-graph-data)
  (def analytics @(subscribe [::db/data-field :get-analytics]))
  (keys analytics)
  (get analytics :statistics)
  (get-in analytics [:centralities :degree_centrality])
  (get-in analytics [:centralities :eigenvector_centrality])
  (def collab @(subscribe [::db/data-field :get-filtered-collab]))
  (count collab)
  (def weighted-collab @(subscribe [::db/data-field :get-weighted-collab]))
  (reduce + (map :weight weighted-collab))
  (count (set (map :rec_id collab)))
  )
