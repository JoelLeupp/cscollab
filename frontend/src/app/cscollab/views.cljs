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
   [app.cscollab.interactive-map :as interactive-map]
   [app.cscollab.transformer :as tf]))


(defn main-view []
  (fn []
    [:<> 
     [acl/section
      #_[acl/title-white "Landscape of Scientific Collaborations"]
      [acl/content
       [filter-panel] 
       [interactive-map/interactive-map]
       #_[tf/collab-count]]]]))

