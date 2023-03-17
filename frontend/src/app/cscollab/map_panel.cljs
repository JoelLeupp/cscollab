(ns app.cscollab.map-panel
  (:require
   [app.common.user-input :refer (input-panel)] 
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
   [lists/sub-header {:subheader "chose collaboration network" :style {:text-align :center}}]
   [i/switch {:id :insti?
              :typo-args {:variant :button}
              :stack-args {:display :flex :justify-content :center :margin :auto}
              :label-off "Authors"
              :label-on "Institutions"}]])

(defn graph-color []
  [:div
   [lists/sub-header {:subheader "set graph coloring" :style {:text-align :center}}] 
   [i/autocomplete
    {:id :color-by
     :label "Color Graph by"
     :option-label :option-label
     :options [{:value :no-coloring :label "No Coloring"}
               {:option-label "Grouped"}
               {:value :area :label "by top area"}
               {:value :subarea :label "by top sub area"}]}]])

(reg-sub
 ::show-node-options
 :<- [::db/data-field :get-weighted-collab]
 (fn [weighted-collab]
   (when weighted-collab
     (let [nodes (vec (clojure.set/union
                       (set (map :node/m weighted-collab))
                       (set (map :node/n weighted-collab))))]
       (mapv #(identity {:value % :label %}) (sort nodes))))))


(defn show-node []
  (let [options (subscribe [::show-node-options])]
    (fn []
      [:div
       [lists/sub-header {:subheader "show node in graph" :style {:text-align :center}}]
       [i/autocomplete
        {:id :color-by
         :label "chose node"
         :options @options}]])))

(defn map-config-panel []
  [input-panel
   {:id :map-config-panel
    :start-closed false
    :header "Graph Configuration and Interaction"
    :collapsable? true
    :content
    [grid/grid
     {:grid-args {:justify-content :space-between}
      :item-args {:elevation 0}
      :content
      [{:xs 3 :content [insti-switch]}
       {:xs 3 :content [show-node]}
       {:xs 3 :content [graph-color]}]}]}])