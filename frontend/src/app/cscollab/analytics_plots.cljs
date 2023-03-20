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


(defn select-institution []
  (let [collab (subscribe [::db/data-field :get-filtered-collab])]
    (fn []
      (let [institutions (clojure.set/union (set (map :b_inst @collab)) (set (map :a_inst @collab)))
            options (sort-by :label (map #(identity {:value % :label %}) institutions))]
        [i/autocomplete
         {:id :select-institution
          :keywordize-values false
          :label "chose an institution"
          :style {:max-width 400}
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
          :style {:max-width 400}
          :options options}]))))

(defn institution-view []
  (let [selected-inst (subscribe [::db/user-input-field :select-institution])
        inst-data (subscribe [::db/data-field :get-publications-node-inst])]
    (add-watch selected-inst ::select-inst
               (fn [_ _ _ selected]
                 (let [config (assoc @(subscribe [::common/filter-config]) "institution" true)]
                   (dispatch [::api/get-publications-node :inst selected config]))))
    (fn []
      [:div
       [select-institution]
       ^{:key [@selected-inst @inst-data]}
       [loading-content :get-publications-node-author
        (when @inst-data
          [selected-info/publication-plot
           {:node-data @inst-data
            :style {:width "100%"}
            :layout {:legend {:y 1.1 :x -0.5}}
            :box-args {:height "60vh" :min-width 700 :width "60%" :overflow :auto :margin-top 2}}])]])))

(defn author-view []
  (let [selected-author (subscribe [::db/user-input-field :select-author])
        author-data (subscribe [::db/data-field :get-publications-node-author])]
    (add-watch selected-author ::select-author
               (fn [_ _ _ selected]
                 (let [config (assoc @(subscribe [::common/filter-config]) "institution" false)]
                   (dispatch [::api/get-publications-node :author selected config]))))
    (fn []
      [:div
       [select-author]
       ^{:key [@selected-author @author-data]}
       [loading-content :get-publications-node-author
        (when @author-data
          [selected-info/publication-plot
           {:node-data @author-data
            :style {:width "100%"}
            :layout {:legend {:y 1.1 :x -0.5}}
            :box-args {:height "60vh" :min-width 700 :width "60%" :overflow :auto :margin-top 2}}])]])))

(comment
  (def collab @(subscribe [::db/data-field :get-filtered-collab])) 
   (assoc @(subscribe [::common/filter-config]) "institution" true)
  (subscribe [::db/user-input-field :select-institution])
  (subscribe [::db/user-input-field :select-author])
  )