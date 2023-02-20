(ns app.cscollab.graph-view
  (:require [reagent.core :as reagent :refer [atom]] 
            [app.cscollab.transformer :as tf]
            [app.cscollab.data :as data]
            [app.components.colors :refer [colors]]
            [app.components.lists :refer [collapse]]
            [app.db :as db]
            [app.common.leaflet :as ll :refer [leaflet-map]]
            [app.cscollab.map-panel :as mp]
            [app.components.button :as button]
            [reagent-mui.material.paper :refer [paper]]
            [leaflet :as L] 
            [app.common.container :refer (viz-container)]
            [re-frame.core :refer
             (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
            [app.util :as util]))


(defn graph-view []
  (let [insti? (subscribe [::ll/insti?])]
    (fn []
      [viz-container 
       {:id :graph-container
        :title "Collaboration Graph"
        :content [:div {:style {:margin 0 :padding 0 :width "100%" :height "100%" :text-align :center}}] 
        :info-component [:div {:style {:display :flex :justify-content :space-between 
                                       :width "100%" :height "100%" :border-style :solid}}
                         [:h3 {:style {:margin 0}} "INFO BOX"]
                         [button/close-button
                          {:on-click  #(dispatch [::db/set-ui-states [:viz :open?] false])}]] 
        :info-open? (subscribe [::db/ui-states-field [:viz :open?]])}])))