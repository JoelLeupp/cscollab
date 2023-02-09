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


(defn collapse [{:keys [sub div args]}]
  [:> mui-collapse
   (util/deep-merge
    {:in @sub
     :timeout :auto
     :unmountOnExit true} args)
   div])

(defn sub-header [{:keys [style subheader args id]}]
  [:> mui-list-sub-header 
   (util/deep-merge 
    {:component :div :id id :sx (util/deep-merge {:font-size 18} style)}
    args)
   subheader])

(defn checkbox-list [{:keys [style list-args id subheader content :namespace-id?]
                      :or {namespace-id? true}}]
  "generate a nested list with checkboxes" 
(let [child-id
      (fn [parent child]
        (if namespace-id?
          (keyword parent child)
          (keyword child))) 
      all-ids
      (set
       (flatten
        (map (fn [c]
               (let [id (:id c)
                     children
                     (when (:children c)
                       (map #(child-id id (:id %)) (:children c)))]
                 (concat [id] children)))
             content)))]
  (add-watch
   (subscribe [::db/user-input-field [id]])
   (keyword id :watcher)
   (fn [_ _ _ new-state]
     (doseq [c content]
       (when (= :all (:id c))
         (if (= (count all-ids) (count new-state))
           (dispatch [::db/set-user-input-selection id :all true])
           (dispatch [::db/set-user-input-selection id :all false])))
       (when (:children c)
         (if
          (clojure.set/subset?
           (set (map #(child-id (:id c) (:id %)) (:children c)))
           new-state)
           (dispatch [::db/set-user-input-selection id (:id c) true])
           (dispatch [::db/set-user-input-selection id (:id c) false]))))))
  (fn [{:keys [style list-args id subheader content]}]
    (let [checked-ids (subscribe [::db/user-input-field id])]
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
             [:> mui-list-sub-header {:component :div :id id :sx (:subheader style)}
              subheader])}))
        list-args)
       (for [c content]
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
              (contains? @checked-ids (:id c)) 
              :indeterminate
              (when (not (contains? @checked-ids (:id c)))
                (cond
                  (and
                   (= :all (:id c))
                   (> (count @checked-ids) 0)
                   (not (= (count all-ids) (count @checked-ids)))) true
                  (and
                   (not (= :all (:id c)))
                   (:children c)
                   (seq (clojure.set/intersection
                         (set (map #(child-id (:id c) (:id %)) (:children c)))
                         @checked-ids))) true))
              :on-change
              (or (:on-change c)
                  (fn [e]
                    (when (= (:id c) :all)
                    ;remove or add all ids/sub-ids
                      (dispatch
                       [::db/set-user-input-selection id
                        all-ids
                        (-> e .-target .-checked)]))
                    (when (:children c)
                    ; remove or add all children
                      (dispatch [::db/set-user-input-selection id
                                 (set (map #(child-id (:id c) (:id %)) (:children c)))
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
                    (contains? @checked-ids (child-id (:id c) (:id sub))) 
                    :on-change
                    (or
                     (:on-change sub)
                     (fn [e]
                       (dispatch [::db/set-user-input-selection id
                                  (child-id (:id c) (:id sub))
                                  (-> e .-target .-checked)])))}]]
                 [:>  mui-list-item-text
                  {:primary (:label sub)
                   :primary-typography-props (:style sub)}]])]])))]))))


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