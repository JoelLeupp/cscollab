(ns app.app
  (:require
   [app.router :as router]
   ["@mui/material/styles" :refer (createTheme ThemeProvider)]
   [app.components.colors :as c] 
   [re-frame.core :refer (dispatch subscribe)]
   [app.cscollab.nav :as nav]
   [app.cscollab.views :as views]))


(def custom-theme 
  (createTheme 
   (clj->js
    {:palette {:primary {:main (c/colors :main)}
               :secondary {:main (c/colors :second)}}})))

(defn routes []
  [["/"
    {:name :home
     :view views/main-view}]
   ["/conference-explorer"
    {:name :conference-explorer
     :view views/conference-explorer}]
   ["/publication-explorer"
    {:name :publication-explorer
     :view views/publication-explorer}]
   ["/author-explorer"
    {:name :author-explorer
     :view views/author-explorer}]
   ["/guide"
    {:name :guide
     :view views/guide}]])


(defn app []
  (let [current-route (subscribe [::router/current-route])]
    (fn []
      [:div {:style {:display :flex :height "100vh" :width "100%" 
                     :background-color (:main c/bg-colors)}}
       [:> ThemeProvider {:theme custom-theme}
        [:main {:style {:width "100%" :background-color (:main c/bg-colors)}}
         [nav/nav-bar]
         [nav/nav-menu]
         (when-let [view-comp (-> @current-route :data :view)]
           [view-comp @current-route])]]])))

(comment
  @(subscribe [::router/current-route])
  )

