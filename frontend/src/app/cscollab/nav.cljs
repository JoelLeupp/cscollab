(ns app.cscollab.nav
  (:require
   [app.components.app-bar :refer (app-bar)]
   [app.components.drawer :refer (drawer)]
   [app.components.lists :refer (menu)]
   [app.router :as router :refer (router)]
   [re-frame.core :refer (dispatch subscribe)]
   [reagent.core :as r]
   ["@mui/icons-material/Home" :default ic-home]
   ["@mui/icons-material/InsertChart" :default ic-chart]
   ["@mui/icons-material/ViewList" :default ic-view-list]
   ["@mui/icons-material/Settings" :default  ic-settings]
   ["@mui/material/Typography" :default mui-typography]))



(defn nav-bar []
  [app-bar {:app-name "Visualization of Scientific Collaboration Networks"
            :right-div [:div {:style {:display :flex :align-items :center}}
                        "University of Zürich"]}])


(defn nav-menu []
  (let [selected (r/atom 1)]
    (fn []
      ^{:key @selected}
      [drawer
       {:content
        [menu
         {:label-id "Menu"
          :subheader "Menu"
          :content
          [{:id 1 :label "Visualization" :icon ic-home :selected (= 1 @selected)
            :on-click #(do
                         (reset! selected 1)
                         (dispatch [::router/navigate :app.app/home]))}
           {:id 2 :label "Conferences" :icon ic-view-list :selected (= 2 @selected)
            :on-click #(do
                         (reset! selected 2)
                         (dispatch [::router/navigate :app.app/conferences]))} 
           #_{:id 3 :label "Settings" :icon ic-settings :selected (= 3 @selected)
            :on-click #(reset! selected 3)}]}]}])))