(ns app.components.app-bar
  (:require 
   ["@mui/material/AppBar" :default mui-app-bar]
   ["@mui/material/Toolbar" :default mui-tool-bar]
   ["@mui/material/Typography" :default mui-typography]
   ["@mui/material/IconButton" :default mui-icon-button]
   ["@mui/icons-material/Menu" :default ic-menu]
   ["@mui/material/Box" :default mui-box]
   [app.util :as util]
   [app.db :as db]
   [reagent.core :as r]
   [re-frame.core :refer (dispatch subscribe)]
   ))

(defn app-bar [{:keys [app-name app-bar-args right-div]}]
  (let [open? (subscribe [::db/ui-states-field :main-menu])]
    (fn [{:keys [app-name app-bar-args right-div]}]
      [:> mui-app-bar
       (util/deep-merge {:position :static #_#_:style {:background-color :white}} app-bar-args)
       [:> mui-tool-bar {:style {:padding 0}}
        [:div {:style {:width "100%"
                       :display :grid
                       :grid-template-columns "minmax(5px, 5%) 1fr minmax(5px, 5%)"}}
         [:> mui-icon-button
          {:on-click #(dispatch [::db/set-ui-states :main-menu (not (util/any->boolean @open?))])
           :color :inherit
           :size :large
           :edge :start
           :aria-label "menu"}
          [:> ic-menu #_{:font-size :large}]]
         [:div {:style {:display :flex :justify-content :space-between}}
          [:> mui-typography {:variant :h4 #_#_:sx {:flex-grow 1}} app-name]
          right-div]]]])))
