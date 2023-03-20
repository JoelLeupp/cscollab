(ns app.cscollab.analytics-plots
  (:require [reagent.core :as reagent :refer [atom]]
            [app.cscollab.data :as data]
            [app.components.colors :refer [colors]]
            [app.components.lists :refer [collapse]]
            [app.db :as db]
            [app.components.grid :as grid]
            [app.cscollab.selected-info :as selected-info]
            [app.components.table :as table]
            [app.common.container :refer (analytics-container)]
            [app.components.stack :refer (horizontal-stack)]
            [app.components.loading :refer (loading-content)]
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

(defn select-perspective [insti?]
  (fn []
    [i/autocomplete
     {:id :select-perspective
      :label "select a view"
      :style {:width 400}
      :options
      (vec
       (concat
        [{:value :publications :label "publications"}]
        (when insti?
          [{:value :authors :label "authors"}])
        [{:value :institutions :label "institutions"}
         {:value :countries :label "countries"}
         {:value :year :label "year"}
         {:value :author-collab :label "collaboration with authors"}]))}]))

(defn select-institution []
  (let [collab (subscribe [::db/data-field :get-filtered-collab])]
    (fn []
      (let [institutions (clojure.set/union (set (map :b_inst @collab)) (set (map :a_inst @collab)))
            options (sort-by :label (map #(identity {:value % :label %}) institutions))]
        [i/autocomplete
         {:id :select-institution
          :keywordize-values false
          :label "chose an institution"
          :style {:width 400}
          :options options}]))))

(defn select-author []
  (let [collab (subscribe [::db/data-field :get-filtered-collab])
        csauthors (subscribe [::db/data-field :get-csauthors])]
    (fn []
      (let [authors (clojure.set/union (set (map :a_pid @collab)) (set (map :b_inst @collab)))
            pid->name (zipmap (map :pid @csauthors) (map :name @csauthors))
            options (sort-by :label (map #(identity {:value % :label (get pid->name % %)}) authors))]
        [i/autocomplete
         {:id :select-author
          :keywordize-values false
          :label "chose an author"
          :style {:width 400}
          :options options}]))))

(defn publication-plot [data]
  [selected-info/publication-plot
   {:node-data data
    :style {:width "100%"}
    :layout {:legend {:y 1.1 :x -0.5}}
    :box-args {:height "60vh" :min-width 700 :width "60%" :overflow :auto :margin-top 2}}])

(defn country-plot [node-data]
  (let [region-mapping @(subscribe [::db/data-field :get-region-mapping])
        country-mapping (zipmap (map :country-id region-mapping) (map :country-name region-mapping))]
    [selected-info/general-collab-plot
     {:node-data node-data
      :key :collab_country
      :style {:width "100%"}
      :name "collaborations by country"
      :box-args {:height "60vh" :min-width 700 :width "60%" :overflow :auto :margin-top 2}
      :mapping country-mapping}]))

(defn year-plot [node-data]
  [selected-info/general-collab-plot
   {:node-data node-data
    :key :year
    :style {:width "100%"}
    :name "collaborations by year"
    :box-args {:height "60vh" :min-width 700 :width "60%" :overflow :auto :margin-top 2}
    :layout {:yaxis {:autorange nil}}}])

(defn inst-plot [node-data]
  [selected-info/general-collab-plot
   {:node-data node-data
    :key :collab_inst
    :style {:width "100%"}
    :box-args {:height "60vh" :min-width 700 :width "60%" :overflow :auto :margin-top 2}
    :name "collaborations by institution"}])

(defn author-table [node-data]
  [selected-info/author-table 
   {:node-data node-data 
    :paper-args {:min-width 700 :width "60%" :justify-content :flex-start :margin-top 2 :margin nil}
    :container-args {:max-height "60vh"}}]
  )

(defn with-author-plot [node-data]
  (let [csauthors @(subscribe [::db/data-field :get-csauthors])
        pid->name (zipmap (map :pid csauthors) (map :name csauthors))]
    [selected-info/general-collab-plot
     {:node-data node-data
      :key :collab_pid
      :style {:width "100%"}
      :box-args {:height "60vh" :min-width 700 :width "60%" :overflow :auto :margin-top 2}
      :name "collaborations by institution"
      :mapping pid->name}]))

(defn institution-view []
  (let [selected-inst (subscribe [::db/user-input-field :select-institution])
        inst-data (subscribe [::db/data-field :get-publications-node-inst])
        perspecitve (subscribe [::db/user-input-field :select-perspective])]
    (add-watch selected-inst ::select-inst
               (fn [_ _ _ selected]
                 (let [config (assoc @(subscribe [::common/filter-config]) "institution" true)]
                   (dispatch [::api/get-publications-node :inst selected config]))))
    (fn []
      [:div
       [:div {:style {:display :flex :justify-content :flex-start :gap 40}}
        [select-institution]
        [select-perspective true]]
       ^{:key [@selected-inst @inst-data @perspecitve]}
       [loading-content :get-publications-node-author
        (when @inst-data
          (case @perspecitve
            :publications [publication-plot @inst-data]
            :authors [author-table @inst-data]
            :institutions [inst-plot @inst-data]
            :countries [country-plot @inst-data]
            :year [year-plot @inst-data]
            :author-collab [with-author-plot @inst-data]
            [publication-plot @inst-data]) 
          )]])))

(defn author-view []
  (let [selected-author (subscribe [::db/user-input-field :select-author])
        author-data (subscribe [::db/data-field :get-publications-node-author])
        perspecitve (subscribe [::db/user-input-field :select-perspective])]
    (add-watch selected-author ::select-author
               (fn [_ _ _ selected]
                 (let [config (assoc @(subscribe [::common/filter-config]) "institution" false)]
                   (dispatch [::api/get-publications-node :author selected config]))))
    (fn []
      [:div
       [:div {:style {:display :flex :justify-content :flex-start :gap 40}}
        [select-author]
        [select-perspective false]] 
       ^{:key [@selected-author @author-data @perspecitve]}
       [loading-content :get-publications-node-author
        (when @author-data
          (case @perspecitve
            :publications [publication-plot @author-data]
            :institutions [inst-plot @author-data]
            :countries [country-plot @author-data]
            :year [year-plot @author-data]
            :author-collab [with-author-plot @author-data]
            [publication-plot @author-data]))]])))

(comment
  (subscribe [::db/user-input-field :select-perspective])
  (def collab @(subscribe [::db/data-field :get-filtered-collab])) 
   (assoc @(subscribe [::common/filter-config]) "institution" true)
  (subscribe [::db/user-input-field :select-institution])
  (subscribe [::db/user-input-field :select-author])
  )