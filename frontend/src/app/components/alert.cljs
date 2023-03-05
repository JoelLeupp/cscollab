(ns app.components.alert
  (:require
   ["@mui/material/Alert" :default mui-alert]
   ["@mui/material/AlertTitle" :default mui-alert-title]
   [app.util :as util]
   [app.db :as db]
   [reagent.core :as r]))

;;severity: info warning sucess error
(defn alert [{:keys [severity alert-args title content action on-close]}]
  [:> mui-alert
   (util/deep-merge
    {:severity (or severity :info)
     :on-close on-close :action action}
    alert-args)
   (when title [:> mui-alert-title [:strong title]])
   content])