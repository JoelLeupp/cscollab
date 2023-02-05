(ns app.views
  (:require
   [app.router :as router]
   ["@mui/material/styles" :refer (createTheme ThemeProvider)]
   [app.components.colors :as c] 
   [re-frame.core :refer (dispatch subscribe)]
   [app.cscollab.nav :as nav]
   [app.cscollab.views :as cscollab]))


(def custom-theme 
  (createTheme 
   (clj->js
    {:palette {:primary {:main (c/colors :main)}
               :secondary {:main (c/colors :second)}}})))

(defn routes []
  [["/" {:name ::home
         :view cscollab/main-view}]])


(defn app []
  (let [current-route (subscribe [::router/current-route])]
    (fn []
      [:div {:style {:display :flex :height "100vh" :width "100%" 
                     :background-color (:main c/bg-colors)}}
       [:> ThemeProvider {:theme custom-theme}
        [:main {:style {:width "100%" :background-color (:main c/bg-colors)}}
         [nav/nav-bar]
         (when-let [view-comp (-> @current-route :data :view)]
           [view-comp @current-route])]]])))

(comment
  @(subscribe [::router/current-route])
  )

