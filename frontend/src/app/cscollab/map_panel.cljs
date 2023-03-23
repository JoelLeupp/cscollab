(ns app.cscollab.map-panel
  (:require
   [app.common.user-input :refer (input-panel)] 
   [app.db :as db]
   [app.util :as util] 
   [app.cscollab.interactive-map :as im]
   [app.components.grid :as grid]
   [app.components.button :refer (button)]
   [app.components.stack :refer (horizontal-stack)]
   [re-frame.core :refer
    (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
   [app.components.lists :as lists]
   [app.common.graph :as g]
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
  [:div {:style {:height "100%"}}
   [lists/sub-header {:subheader "collaboration network" :style {:text-align :left :padding 0}}]
   [i/switch {:id :insti? 
              :typo-args {:variant :button}
              :stack-args {:display :flex :justify-content :flex-start :margin :auto :style {:height 56}}
              :label-off "Authors"
              :label-on "Institutions"}]])

(defn graph-color []
  [:div
   [lists/sub-header {:subheader "set graph coloring" :style {:text-align :left :padding 0}}] 
   [i/autocomplete
    {:id :color-by
     :label "Color Graph by"
     :option-label :option-label
     :options [{:value :no-coloring :label "No Coloring"} 
               {:value :area :label "by top area"}
               {:value :subarea :label "by top sub area"}
               {:value :degree-centrality :label "degree centrality"}
               {:value :eigenvector-centrality :label "eigenvector centrality"}]}]])

(reg-sub
 ::show-node-options
 :<- [::db/data-field :get-weighted-collab]
 :<- [::db/data-field :get-csauthors]
 (fn [[weighted-collab csauthors]]
   (when (and weighted-collab csauthors)
     (let [nodes (vec (clojure.set/union
                       (set (map :node/m weighted-collab))
                       (set (map :node/n weighted-collab)))) 
           pid->name (zipmap (map :pid csauthors) (map :name csauthors))]
       (sort-by :label (map #(identity {:value % :label (get pid->name % %)}) nodes))))))

(defn show-node []
  (let [tab-view (subscribe [::db/ui-states-field [:tabs :viz-view]])
        options (subscribe [::show-node-options])
        node (subscribe [::db/user-input-field [:show-node]])]
    (fn []
      [:div
       [lists/sub-header {:subheader "show node in graph" :style {:text-align :left :padding 0}}]
       [horizontal-stack
        {:items
         (list
          [i/autocomplete
           {:id :show-node
            :keywordize-values false
            :label "chose node"
            :style {:width "70vw"}
            :options @options}]
          [button {:text "show"
                   :on-click  #(case @tab-view
                                 :map (im/show-node @node)
                                 :graph (g/show-ele @node)
                                 nil)}])}]])))

(defn map-config-panel []
  [input-panel
   {:id :map-config-panel
    :start-closed false
    :header "Graph Configuration and Interaction"
    :collapsable? true
    :content
    [grid/grid
     {:grid-args {:justify-content :space-evenly}
      :item-args {:elevation 0}
      :content
      [{:xs 3 :content [insti-switch]}
       {:xs 3 :content [show-node]}
       {:xs 3 :content [graph-color]}]}]}])
