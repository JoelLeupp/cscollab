(ns app.components.lists
  (:require
   [app.util :as util]
   [app.db :as db]
   [reagent.core :as r]
   ["@mui/material/ListSubheader" :default mui-list-sub-header]
   ["@mui/material/List" :default mui-list]
   [re-frame.core :refer (dispatch subscribe)]
   [app.components.colors :as c]
   ["@mui/material/ListItem" :default mui-list-item]
   ["@mui/material/ListItemButton" :default mui-list-item-button]
   ["@mui/material/ListItemIcon" :default mui-list-item-icon]
   ["@mui/material/ListItemText" :default mui-list-item-text]
   ["@mui/icons-material/ExpandLess" :default ic-expand-less]
   ["@mui/icons-material/ExpandMore" :default ic-expand-more]))

(defn menu [{:keys [style list-args label-id subheader content content-sub]}]
  (fn [{:keys [style list-args label-id subheader content content-sub]}]
    [:> mui-list
     (util/deep-merge
      (merge
       {:sx (util/deep-merge
             {:width "100%"
              :max-width 360
              #_#_:bgcolor (:main c/colors)} style)
        :component :nav
        :aria-labelledby label-id}
       (when subheader
         {:subheader
          (r/as-element
           [:> mui-list-sub-header {:component :div :id label-id}
            subheader])}))
      list-args)
     (for [c (if content-sub @content-sub content)]
       ^{:key (:id c)}
       [:> mui-list-item-button
        {:selected (:selected c)
         :on-click (:on-click c)}
        (when (:icon c) 
          [:> mui-list-item-icon [:> (:icon c)]])
        [:>  mui-list-item-text {:primary (:label c)}]])]))