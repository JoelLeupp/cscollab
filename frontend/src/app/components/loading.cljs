(ns app.components.loading
  (:require
   ["@mui/material/CircularProgress" :default mui-circular-progress]
   ["@mui/material/Box" :default mui-box]
   [app.util :as util]
   [app.db :as db]
   [re-frame.core :refer
    (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
   [reagent.core :as r]))

(defn loading-content [loading-id content]
  (let [loading? (subscribe [::db/loading? loading-id])]
    (fn []
      (if @loading?
        [:> mui-box {:sx {:display :flex :justify-content :center :align-items :center :height "300px"}}
         [:> mui-circular-progress {:size 70}]]
        content))))
