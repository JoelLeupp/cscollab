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


#_(defn list-test []
  [:> mui-list {:dense false :sx {:max-width 350 :width "100%"}} 
   [:> mui-list-item-button
    {:key :a :disablePadding true}
    [:> mui-list-item-icon
     [:> mui-checkbox
      {:checked  @(subscribe [::db/user-input-field [:list :a]])
       :on-click
       (fn []
         (dispatch [::db/update-user-input [:list :a]
                    #(if % false true)]))}]]
    [:>  mui-list-item-text {:primary "list item a" 
                             :primary-typography-props 
                             {:font-size 20
                              :font-weight :medium}}]] 
   [:> mui-list-item-button
    {:key :b :disablePadding true
     :on-click
     (fn []
       (dispatch [::db/update-ui-states [:list :open]
                  #(if % false true)]))}
    [:> mui-list-item-icon
     [:> mui-checkbox
      {:checked  @(subscribe [::db/user-input-field [:list :b]])
       :on-click
       (fn []
         (dispatch [::db/update-user-input [:list :b]
                    #(if % false true)]))}]]
    [:>  mui-list-item-text {:primary "list item b"}]
    (if @(subscribe [::db/ui-states-field [:list :open]])
      [:> ic-expand-less]
      [:> ic-expand-more])]
   [:> mui-collapse {:in @(subscribe [::db/ui-states-field [:list :open]])
                     :timeout :auto
                     :unmountOnExit true}
    [:> mui-list {:dense false}
     [:> mui-list-item-button
      {:key :a :disablePadding true :sx {:pl 4}}
      [:> mui-list-item-icon
       [:> mui-checkbox
        {:checked  @(subscribe [::db/user-input-field [:list :c]])
         :on-click
         (fn []
           (dispatch [::db/update-user-input [:list :c]
                      #(if % false true)]))}]]
      [:>  mui-list-item-text {:primary "list item c"}]]]]
   ])

(def content [{:id :a :label "main item" :style {:font-size 20
                                                 :font-weight :medium}}
              {:id :b :label "second item" :children [{ :id :c :label "sub-item"}]}])

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
        [:> mui-list-item-button
         {:key (:id c) :disablePadding true
          :on-click
          (or (:on-click c)
              (when (:children c)
                (fn []
                  (dispatch [::db/update-ui-states (conj [id (:id c)] :open?)
                             #(if % false true)]))))}
         [:> mui-list-item-icon
          [:> mui-checkbox
           {:checked  @(subscribe [::db/user-input-field [id (:id c)]])
            :on-click
            (or (:on-click c)
                (fn []
                  (dispatch [::db/update-user-input [id (:id c)]
                             #(if % false true)])))}]]
         [:>  mui-list-item-text {:primary (:label c)
                                  :primary-typography-props (:style c)}]
         (when (:children c)
           (if @(subscribe [::db/ui-states-field (conj [id (:id c)] :open?)])
             [:> ic-expand-less]
             [:> ic-expand-more]))]
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
                  :checked  @(subscribe [::db/user-input-field
                                         [id (keyword (:id c) (:id sub))]])
                  :on-click
                  (or (:on-click sub)
                      (fn []
                        (dispatch [::db/update-user-input
                                   [id (keyword (:id c) (:id sub))]
                                   #(if % false true)])))}]]
               [:>  mui-list-item-text
                {:primary (:label sub)
                 :primary-typography-props (:style sub)}]])]])))]))

(defn list-test []
  [checkbox-list
   {:id :nested-list
    :list-args {:dense false :sx {:max-width 350 :width "100%"}}
    :content content}]
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