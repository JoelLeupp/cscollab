(ns app.cscollab.map-panel
  (:require
   [app.common.user-input :refer (input-panel)]
   [app.cscollab.data :as data]
   [app.db :as db]
   [app.util :as util]
   [app.components.grid :as grid]
   [re-frame.core :refer
    (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
   [app.components.lists :as lists]
   [app.components.inputs :as i]))


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

(defn insti-switch []
  "show institutional collaboration or author collaboration"
  [:div
   [:h3 {:style {:text-align :center :margin-bottom 20}} "Collaboration Network"]
   [i/switch {:id :insti?
              :typo-args {:variant :button}
              :stack-args {:display :flex :justify-content :center :margin :auto}
              :label-off "Authors"
              :label-on "Institutions"}]])

(defn graph-color []
  [:div
   [:h3 {:style {:text-align :center :margin-bottom 20}} "Graph Coloring"]
   [i/autocomplete
    {:id :color-by
     :label "Color Graph by"
     :options [{:value :no-coloring :label "No Coloring"}
               {:value :area :label "by top area"}
               {:value :subarea :label "by top sub area"}]}]])

(defn map-config-panel []
  [input-panel
   {:id :map-config-panel
    :start-closed true
    :header "Map Configuration and Interaction"
    :collapsable? true
    :content
    [grid/grid
     {:grid-args {:justify-content :space-evenly}
      :item-args {:elevation 0}
      :content
      [{:xs 3 :content [insti-switch]}
       {:xs 3 :content [graph-color]}]}]}])