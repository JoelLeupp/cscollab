(ns app.components.drawer
  (:require
   [app.util :as util]
   [app.db :as db]
   [reagent.core :as r]
   [re-frame.core :refer (dispatch subscribe)]
   ["@mui/material/Drawer" :default mui-drawer]
   ["@mui/material/Box" :default mui-box]
   ["@mui/material/Divider" :default mui-divider]
   [app.components.colors :as c]
   ["@mui/material/IconButton" :default mui-icon-button]
   ["@mui/icons-material/ChevronLeft" :default ic-checron-left]))

(def drawer-width 240)

(defn close-drawer [id open?]
  #(dispatch [::db/set-ui-states id  (not (util/any->boolean open?))]))

(defn drawer-header [id open?]
  [:> mui-icon-button
   {:on-click (close-drawer id open?)
    :style {:display :flex
            :align-items :center
            :justify-content :flex-end
            :padding "15px"
            :width "100%"}}
   [:> ic-checron-left]])

(defn drawer [{:keys [content ref-id anchor drawer-args]
               :or {ref-id :main-menu}}]
  (let [open? (subscribe [::db/ui-states-field ref-id])]
    (fn [{:keys [ref-id anchor drawer-args]
          :or {ref-id :main-menu}}]
      [:> mui-drawer
       (util/deep-merge
        {:sx {:width drawer-width
              "& .MuiDrawer-paper" {#_#_:background-color (:main c/colors)
                                    :width drawer-width}}
         :anchor (or anchor :left)
         :open @open?
         :on-close (close-drawer ref-id @open?)}
        drawer-args)
       [:<>
        [drawer-header ref-id @open?]
        [:> mui-divider]
        content]])))

(comment
  @(subscribe [::db/ui-states-field :main-menu]))