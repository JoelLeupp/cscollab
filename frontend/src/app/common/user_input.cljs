(ns app.common.user-input
  "panels and forms for user-inputs"
  (:require
   ["@mui/material/Collapse" :default mui-collapse]
   ["@mui/material/Card" :default mui-card]
   ["@mui/material/CardHeader" :default mui-card-header]
   ["@mui/material/CardContent" :default mui-card-content]
   ["@mui/material/CardActions" :default mui-card-actions]
   ["@mui/material/Grid" :default mui-grid]
   ["@mui/icons-material/ExpandLess" :default ic-expand-less]
   ["@mui/icons-material/ExpandMore" :default ic-expand-more]
   ["@mui/material/IconButton" :default mui-icon-button]
   [app.util :as util]
   [app.db :as db]
   [reagent.core :as r]
   [re-frame.core :refer (dispatch subscribe)]))

(defn input-panel 
  [{:keys [id start-closed]}]
  "container for user inputs" 
  (when start-closed
    (dispatch [::db/set-ui-states (keyword id :closed?) true]))
  (let  [closed? (subscribe [::db/ui-states-field (keyword id :closed?)])] 
    (fn [{:keys [id components collapsable? card-args header header-args content-args content]}]
      [:> mui-card (util/deep-merge
                    {:square true
                     :elevation 0
                     :style {:margin-top 15 :margin-bottom 15}}
                    card-args)
       [:> mui-card-header
        (util/deep-merge
         {:on-click (if collapsable?
                      #(dispatch [::db/set-ui-states (keyword id :closed?) (not (util/any->boolean @closed?))])
                      (fn []))
          :style {:padding-top 4 :padding-bottom 4}
          :subheader
          (r/as-element
           [:> mui-grid {:container true :justify-content :space-between
                         :align-items :center}
            [:> mui-grid {:item true} header]
            [:> mui-grid {:item true}
             (when collapsable?
               [:> mui-icon-button {:size :small}
                (if @closed? [:> ic-expand-more] [:> ic-expand-less])])]])}
         header-args)]
       [:> mui-collapse {:in  (not (if (nil? @closed?) start-closed @closed?))}
        [:> mui-card-content
         (or content
             [:div (util/deep-merge
                    {:style {:container true
                             :display :grid
                             :column-gap 30
                             :row-gap 15
                             :z-index 1001
                             :grid-template-columns "repeat(auto-fill, minmax(250px, 1fr))"
                             :justify-content nil
                             :align-items :flex-start}}
                    content-args)
              (for [c components] c)])]]])))

(comment
  (dispatch [::db/set-ui-states :input-panel true])
  (subscribe [::db/ui-states])
  )
