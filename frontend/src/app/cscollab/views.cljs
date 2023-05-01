(ns app.cscollab.views
  (:require
   [app.components.layout :as acl]
   [app.common.user-input :refer (input-panel)]
   [re-frame.core :refer (dispatch subscribe)]
   #_["react-lorem-ipsum" :refer (loremIpsum)]
   #_[app.components.lists :as lists]
   [app.db :as db]
   [app.cscollab.panels.filter-panel :refer (data-selection-panel filter-panel)]
   [app.components.drawer :refer (drawer)]
   ["@mui/material/Typography" :default mui-typography]
   [app.util :as util]
   [app.components.stack :refer (horizontal-stack)]
   ["@mui/material/IconButton" :default mui-icon-button] 
   ["@mui/icons-material/ChevronLeft" :default mui-chevron-left]
   ["@mui/icons-material/ChevronRight" :default mui-chevron-right]
   ["@mui/icons-material/ExpandLess" :default ic-expand-less]
   ["@mui/icons-material/ExpandMore" :default ic-expand-more]
   [app.router :as router :refer (router)]
   [reagent.core :as r]
   [app.components.button :refer (button)]
   [app.cscollab.view.conference.conferences :refer (conferences-view)]
   [app.cscollab.view.authors.authors :refer (author-view)]
   [app.cscollab.panels.map-panel :refer (map-config-panel)]
   [app.components.tabs :as tabs]
   [app.cscollab.view.visualization.map.interactive-map :as interactive-map]
   [app.cscollab.view.publications.publications :refer (publication-view update-data)]
   [app.cscollab.view.visualization.graph.graph-view :refer (graph-view graph-update)]
   [app.cscollab.view.visualization.analytics.analytics :refer (analytics-view get-analytics-graph-data)]
   [app.cscollab.view.guide.guide :refer (guide-view)]))


(defn config-panels []
  (let [tab-view (subscribe [::db/ui-states-field [:tabs :viz-view]])]
    (fn [] 
      [map-config-panel
       #(case @tab-view
          :map (interactive-map/update-event)
          :graph (graph-update)
          :analytics (get-analytics-graph-data)
          nil)]
      #_[input-panel
         {:id :config-panels
          :start-closed true
          :header "Graph Filteres, Configurations and Interactions"
          :collapsable? true
          :content [:div
                    #_[filter-panel]
                    [map-config-panel]
                    [:div {:style {:display :flex :justify-content :center}}
                     [button {:text "apply"
                              :on-click  #(case @tab-view
                                            :map (interactive-map/update-event)
                                            :graph (graph-update)
                                            :analytics (get-analytics-graph-data)
                                            nil)}]]]}])))

(defn data-selection []
  (let [open? (subscribe [::db/ui-states-field :data-selection])
        tab-view (subscribe [::db/ui-states-field [:tabs :viz-view]])
        current-route (subscribe [::router/current-route])]
    (fn []
      [:<>
       [button {:text [:> mui-typography {:sx {:textOrientation :sideways :writingMode :vertical-lr}
                                          :font-size 18} "Data Selection"] 
                :on-click  #(dispatch [::db/set-ui-states :data-selection (not (util/any->boolean @open?))])
                :sx {:position :absolute :right 0 :top 0 :bottom 0 :margin :auto :height 200}
                :startIcon (r/as-element [:> mui-chevron-left #_{:font-size :large}])}]
       #_[:> mui-icon-button
        {:on-click #(dispatch [::db/set-ui-states :data-selection (not (util/any->boolean @open?))])
         :sx {:position :absolute :right 0 :top 0 :bottom 0 :margin :auto}
         :size :large 
         :aria-label "data-selection"}
        [:> ic-expand-more #_{:font-size :large}]]
       [drawer
        {:ref-id :data-selection
         :anchor :right
         :drawer-args {:elevation 0
                       :sx {:width nil
                            :height "100%" 
                            :background-color :transparent
                            :align-items :center
                            "& .MuiDrawer-paper" {:height nil 
                                                  :background-color :transparent
                                                  :width nil}}}
         :costume-content
         [horizontal-stack
          {:box-args {:height "100%" :width "100%" :display :flex}
           :stack-args {:spacing 0}
           :items
           (list
            [:div {:style {:height "100%" :background-color :transparent :display :flex :align-items :center}}
             [button {:text [:> mui-typography {:sx {:textOrientation :sideways :writingMode :vertical-lr}
                                                     :font-size 18} "Data Selection"]
                           :on-click  #(dispatch [::db/set-ui-states :data-selection (not (util/any->boolean @open?))])
                           :sx {:height 200}
                           :endIcon (r/as-element [:> mui-chevron-right #_{:font-size :large}])}]]
            [:div {:style {:height "100%" :width "100%" :background-color :white }} 
             [data-selection-panel 
              #(if (= :home (get-in @current-route [:data :name] :home))
                 (case @tab-view
                   :map (interactive-map/update-event)
                   :graph (graph-update)
                   :analytics (get-analytics-graph-data)
                   (update-data))
                 (update-data))]])}]}]])))

(defn main-view [] 
  (let [tab-view (subscribe [::db/ui-states-field [:tabs :viz-view]])]
    (fn []
      [:<>
       [acl/section
        #_[acl/title-white "Landscape of Scientific Collaborations"]
        [acl/content 
         [data-selection]
         [config-panels]
         [tabs/main-tab
          {:id :viz-view
           :box-args {:margin-bottom "5px"}
           :choices [{:label "Map" :value :map}
                     {:label "Graph" :value :graph}
                     {:label "Analytics" :value :analytics}]}] 
         (case @tab-view
           :map [interactive-map/interactive-map]
           :graph [graph-view]
           :analytics [analytics-view]
           [interactive-map/interactive-map])]]])))


(defn conference-explorer [] 
  (fn []
    [:<>
     [acl/section
      [acl/title-white "Conference Explorer"] 
      [acl/content 
       [conferences-view]]]]))

(defn publication-explorer []
  (fn []
    [:<>
     [acl/section
      [acl/title-white "Publication Explorer"]
      [acl/content
       [data-selection]
       [publication-view]]]]))

(defn author-explorer []
  (fn []
    [:<>
     [acl/section
      [acl/title-white "Author Explorer"]
      [acl/content
       [data-selection]
       [author-view]]]]))

(defn guide []
  (fn []
    [:<>
     [acl/section
      [acl/title-white "Application Guide"]
      [acl/content
       [guide-view]]]]))



