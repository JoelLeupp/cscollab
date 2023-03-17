(ns app.cscollab.views
  (:require
   [app.components.layout :as acl] 
   [app.common.user-input :refer (input-panel)]
   [re-frame.core :refer (dispatch subscribe)]
   #_["react-lorem-ipsum" :refer (loremIpsum)]
   #_[app.components.lists :as lists]
   [app.db :as db]
   [app.cscollab.filter-panel :refer (filter-panel)] 
   [reagent.core :as r] 
   [app.cscollab.interactive-map :as interactive-map]
   [app.cscollab.conferences :refer (conferences-view)]
   [app.cscollab.map-panel :refer (map-config-panel)]
   [app.components.tabs :as tabs]  
   [app.cscollab.graph-view :refer (graph-view)]
   [app.cscollab.analytics :refer (analytics-view)]
   ))


(defn config-panels []
  [input-panel
   {:id :config-panels
    :start-closed true
    :header "Graph Filteres, Configurations and Interactions"
    :collapsable? true
    :content [:div
              [filter-panel]
              [map-config-panel]]}])

(defn main-view [] 
  (let [tab-view (subscribe [::db/ui-states-field [:tabs :viz-view]])]
    (fn []
      [:<>
       [acl/section
        #_[acl/title-white "Landscape of Scientific Collaborations"]
        [acl/content 
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


(defn conferences [] 
  (fn []
    [:<>
     [acl/section
      [acl/title-white "Computer Science Conferences"] 
      [acl/content 
       [conferences-view]]]]))

