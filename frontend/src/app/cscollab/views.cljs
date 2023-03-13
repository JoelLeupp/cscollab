(ns app.cscollab.views
  (:require
   [app.components.layout :as acl]
   [app.components.colors :as c]
   [app.components.inputs :as i]
   [app.common.user-input :refer (input-panel)]
   [re-frame.core :refer (dispatch subscribe)]
   ["react-lorem-ipsum" :refer (loremIpsum)]
   [app.components.lists :as lists]
   [app.db :as db]
   [app.cscollab.filter-panel :refer (filter-panel)] 
   [reagent.core :as r]
   [app.components.lists :refer [collapse]]
   [app.cscollab.interactive-map :as interactive-map]
   [app.cscollab.conferences :refer (conferences-view)]
   [app.cscollab.map-panel :refer (map-config-panel)]
   [app.components.tabs :as tabs]
   [app.components.table :refer (test-table)]
   [reagent-mui.material.paper :refer [paper]]
   [app.common.plotly :as pp :refer (test-plot)]
   [app.cscollab.graph-view :refer (graph-view)]
   [app.cscollab.analysis :refer (analysis-view)]
   [app.cscollab.transformer :as tf]))


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
                     {:label "Analysis" :value :analysis}]}] 
         (case @tab-view
           :map [interactive-map/interactive-map]
           :graph [graph-view]
           :analysis [analysis-view]
           [interactive-map/interactive-map])]]])))


(defn conferences []
  (fn []
    [:<>
     [acl/section
      [acl/title-white "Computer Science Conferences"] 
      [acl/content 
       [conferences-view]]]]))

