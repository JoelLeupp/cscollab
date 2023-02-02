(ns app.components.lists
  (:require
   [app.util :as util]
   [app.db :as db]
   [reagent.core :as r]
   ["@mui/material/ListSubheader" :default mui-list-sub-header]
   ["@mui/material/List" :default mui-list]
   [re-frame.core :refer (dispatch subscribe)]
   [app.components.colors :as c]
   ["@mui/material/Checkbox" :default mui-checkbox]
   ["@mui/material/ListItem" :default mui-list-item]
   ["@mui/material/ListItemButton" :default mui-list-item-button]
   ["@mui/material/ListItemIcon" :default mui-list-item-icon]
   ["@mui/material/ListItemText" :default mui-list-item-text]
   ["@mui/icons-material/ExpandLess" :default ic-expand-less]
   ["@mui/icons-material/ExpandMore" :default ic-expand-more]
   ["@mui/material/Collapse" :default mui-collapse]
   ["@mui/material/IconButton" :default mui-icon-button]))



(defn checkbox-list [{:keys [style list-args id subheader content]}]
  "generate a nested list with checkboxes"
  (fn [{:keys [style list-args id subheader content content-sub]}]
    ^{:key [@(subscribe [::db/user-input-field [id]])
            @(subscribe [::db/ui-states-field [id]])]} 
    [:> mui-list
     (util/deep-merge
      (merge
       {:sx (util/deep-merge
             {:width "100%"
              :max-width 360
              #_#_:bgcolor (:main c/colors)} style)
        :component :nav
        :aria-labelledby id}
       (when subheader
         {:subheader
          (r/as-element
           [:> mui-list-sub-header {:component :div :id id}
            subheader])}))
      list-args)
     (for [c (if content-sub @content-sub content)]
       (list
        [:> mui-list-item
         {:key (:id c) :disablePadding true
          :secondary-action
          (when (:children c)
            (r/as-element
             [:> mui-icon-button
              {:on-click
               (or (:on-click c)
                   (fn []
                     (dispatch [::db/update-ui-states (conj [id (:id c)] :open?)
                                #(if % false true)])))}
              (if @(subscribe [::db/ui-states-field (conj [id (:id c)] :open?)])
                [:> ic-expand-less]
                [:> ic-expand-more])]))}
         [:> mui-list-item-icon
          [:> mui-checkbox
           {:checked  
            (contains? @(subscribe [::db/user-input-field id]) (:id c)) 
            #_@(subscribe [::db/user-input-field [id (:id c)]])
            :on-change
            (or (:on-change c)
                (fn [e]
                  (when (= (:id c) :all)
                    ;remove or add all ids/sub-ids
                    (dispatch
                     [::db/set-user-input-selection id
                      (set
                       (flatten
                        (map (fn [c]
                               (let [id (:id c)
                                     children
                                     (when (:children c)
                                       (map #(keyword id (:id %)) (:children c)))]
                                 (concat [id] children)))
                             (if content-sub @content-sub content))))
                      (-> e .-target .-checked)]))
                  (when (:children c)
                    ; remove or add all children
                    (dispatch [::db/set-user-input-selection id
                               (set (map #(keyword (:id c) (:id %)) (:children c)))
                               (-> e .-target .-checked)]))
                  (dispatch [::db/set-user-input-selection id
                             (:id c)
                             (-> e .-target .-checked)])
                  #_(dispatch [::db/update-user-input [id (:id c)]
                               #(if % false true)])))}]]
         [:>  mui-list-item-text {:primary (:label c)
                                  :primary-typography-props (:style c)}]]
        (when (:children c)
          [:> mui-collapse
           {:in @(subscribe [::db/ui-states-field (conj [id (:id c)] :open?)])
            :timeout :auto
            :unmountOnExit true}
           [:> mui-list {:dense false}
            (for [sub (:children c)]
              [:> mui-list-item-button
               {:key [(:id c) (:id sub)] :disablePadding true :sx {:pl 4}}
               [:> mui-list-item-icon
                [:> mui-checkbox
                 {:indeterminate false
                  :checked  
                  (contains? @(subscribe [::db/user-input-field id]) (keyword (:id c) (:id sub)))
                  #_@(subscribe [::db/user-input-field
                                 [id (keyword (:id c) (:id sub))]])
                  :on-change
                  (or (:on-change sub)
                      (fn [e]
                        (dispatch [::db/set-user-input-selection id
                                   (keyword (:id c) (:id sub))
                                   (-> e .-target .-checked)])
                        #_(dispatch [::db/update-user-input [id (:id c)]
                                     #(if % false true)]))
                      #_(fn []
                          (dispatch [::db/update-user-input
                                     [id (keyword (:id c) (:id sub))]
                                     #(if % false true)])))}]]
               [:>  mui-list-item-text
                {:primary (:label sub)
                 :primary-typography-props (:style sub)}]])]])))]))
(comment
  (def content @(subscribe [:app.cscollab.filter-panel/area-checkbox-content]))
  (set 
   (flatten
        (map (fn [c]
               (let [id (:id c)
                     children
                     (when (:children c)
                       (map #(keyword id (:id %)) (:children c)))]
                 (concat [id] children)))
             content)))
  (def c (second content))
  (clojure.set/difference #{1 2 3} #{2 1})
  (set? #{})
  (when (:children c) 
    (map #(keyword (:id c) (:id %)) (:children c)))
  @(subscribe [::db/user-input-field [:area-checkbox]])
  )

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