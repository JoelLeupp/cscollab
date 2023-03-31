(ns app.cscollab.view.guide.guide
  (:require
   [app.components.layout :as acl]
   [app.components.colors :as colors]
   [app.router :as router]))

(defn guide-view []
  [acl/section {:color :white :style {:font-size 20}}
   [:div {:style {:padding-left 10 :padding-right 10 :width "100%"}}
    [:span "example text"]]])