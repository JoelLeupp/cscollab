(ns app.cscollab.map-panel
  (:require
   [app.common.user-input :refer (input-panel)]
   [app.cscollab.data :as data]
   [app.db :as db]
   [app.util :as util]
   [app.components.grid :as grid]
   [re-frame.core :refer
    (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
   [app.components.lists :as lists]
   [app.components.inputs :as i]))


(defn insti-switch []
  "show institutional collaboration or author collaboration"
  [:div
   [:h3 {:style {:text-align :center :margin 0}} "collaboration between"]
   [i/switch {:id :insti?
              :typo-args {:variant :button}
              :stack-args {:display :flex :justify-content :center :margin :auto}
              :label-off "Authors"
              :label-on "Institutions"}]])

(defn map-config-panel []
  [input-panel
   {:id :map-config-panel
    :start-closed true
    :header "Map Configuration and Interaction"
    :collapsable? true
    :content
    [grid/grid
     {:grid-args {:justify-content :start}
      :item-args {:elevation 0}
      :content
      [{:xs 3 :content [insti-switch]}]}]}])