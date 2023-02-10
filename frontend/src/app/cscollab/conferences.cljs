(ns app.cscollab.conferences
  (:require 
   [app.cscollab.data :as data]
   [app.components.lists :refer [collapse]]
   [app.db :as db]
   [app.components.button :as button]
   [reagent-mui.material.paper :refer [paper]]
   [app.cscollab.filter-panel :refer (filter-panel-conferences)]
   [re-frame.core :refer
    (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]))


(defn conferences-view []
  (fn []
    [filter-panel-conferences]))