(ns app.cscollab.views
  (:require
   [app.components.layout :as acl]
   [app.components.colors :as c]
   [app.components.inputs :as i]
   [app.common.user-input :refer (input-panel)]
   [re-frame.core :refer (dispatch subscribe)]
   ["react-lorem-ipsum" :refer (loremIpsum)]
   [app.db :as db]
   [reagent.core :as r]))


(defn main-view []
  (fn []
    [:<> 
     [acl/section
      [acl/title-white "Landscape of Scientific Collaborations"]
      [acl/content
       [input-panel {:id :input-panel
                     :start-closed false
                     :header "Filters"
                     :collapsable? true
                     :content-args {:style
                                    {:grid-template-columns "repeat(2, minmax(250px, 1fr))"}}
                     :components nil}]]]]))

