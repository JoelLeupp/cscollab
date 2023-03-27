(ns app.cscollab.nav
  (:require
   [app.components.app-bar :refer (app-bar)]
   [app.components.drawer :refer (drawer)]
   [app.components.lists :refer (menu)]
   [app.router :as router :refer (router)]
   [re-frame.core :refer (dispatch subscribe)]
   [reagent.core :as r]
   ["@mui/icons-material/Home" :default ic-home]
   ["@mui/icons-material/Person" :default ic-person]
   ["@mui/icons-material/Article" :default ic-article]
   ["@mui/icons-material/Help" :default ic-help]
   ["@mui/icons-material/InsertChart" :default ic-chart]
   ["@mui/icons-material/ViewList" :default ic-view-list]
   ["@mui/icons-material/Settings" :default  ic-settings]
   ["@mui/material/Typography" :default mui-typography]))



(defn nav-bar []
  [app-bar {:app-name "Visualization of Scientific Collaboration Networks"
            :right-div [:div {:style {:display :flex :align-items :center}}
                        "University of Zürich"]}])


(defn nav-menu []
  (let [current-route (subscribe [::router/current-route])]
    (fn [] 
      (let [view (get-in @current-route [:data :name] :home)]
        ^{:key view}
        [drawer
         {:content
          [menu
           {:label-id "Menu"
            :subheader "Menu"
            :content
            [{:label "Visualization" :icon ic-home :selected (= :home view)
              :on-click #(dispatch [::router/navigate :home])}
             {:label "Authors" :icon ic-person :selected (= :author-explorer view)
              :on-click #(dispatch [::router/navigate :author-explorer])}
             {:label "Conferences" :icon ic-view-list :selected (= :conference-explorer view)
              :on-click #(dispatch [::router/navigate :conference-explorer])} 
             {:label "Publications" :icon ic-article :selected (= :publication-explorer view)
              :on-click #(dispatch [::router/navigate :publication-explorer])} 
             {:label "Guide" :icon ic-help :selected (= :guide view)
              :on-click #(dispatch [::router/navigate :guide])}]}]}]))))