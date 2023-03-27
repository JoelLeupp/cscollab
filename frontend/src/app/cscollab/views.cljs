(ns app.cscollab.views
  (:require
   [app.components.layout :as acl] 
   [app.common.user-input :refer (input-panel)]
   [re-frame.core :refer (dispatch subscribe)] 
   #_["react-lorem-ipsum" :refer (loremIpsum)]
   #_[app.components.lists :as lists]
   [app.db :as db]
   [app.cscollab.panels.filter-panel :refer (filter-panel)] 
   [reagent.core :as r] 
   [app.cscollab.view.visualization.map.interactive-map :as interactive-map]
   [app.cscollab.view.conference.conferences :refer (conferences-view)]
   [app.cscollab.view.authors.authors :refer (author-view)]
   [app.cscollab.panels.map-panel :refer (map-config-panel)]
   [app.components.tabs :as tabs]  
   [app.cscollab.view.visualization.graph.graph-view :refer (graph-view)]
   [app.cscollab.view.visualization.analytics.analytics :refer (analytics-view)]
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


(defn conference-explorer [] 
  (fn []
    [:<>
     [acl/section
      [acl/title-white "Conference List"] 
      [acl/content 
       [conferences-view]]]]))

(defn publication-explorer []
  (fn []
    [:<>
     [acl/section
      [acl/title-white "Publication Explorer"]
      [acl/content
       [:h1 "publication"]]]]))

(defn author-explorer []
  (fn []
    [:<>
     [acl/section
      [acl/title-white "Author Explorer"]
      [acl/content
       [author-view]]]]))

(defn guide []
  (fn []
    [:<>
     [acl/section
      [acl/title-white "Application Guide"]
      [acl/content
       [:h1 "guide"]]]]))



