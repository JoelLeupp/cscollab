(ns app.cscollab.selected-info
  (:require [reagent.core :as reagent :refer [atom]]
            [app.cscollab.transformer :as tf]
            [app.cscollab.data :as data]
            [app.cscollab.filter-panel :as filter-panel]
            [app.components.colors :refer [colors area-color sub-area-color]]
            [app.components.lists :refer [collapse]]
            [cljs-bean.core :refer [bean ->clj ->js]]
            [app.db :as db]
            [app.components.feedback :as feedback]
            [app.common.graph :as g]
            [app.cscollab.map-panel :as mp]
            [app.components.button :as button]
            [reagent-mui.material.paper :refer [paper]] 
            [app.cscollab.common :as common]
            [app.cscollab.api :as api] 
            [app.components.loading :refer (loading-content)]
            [re-frame.core :refer
             (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
            [app.util :as util]))


(defn selected-node-info []
  (let [node-data @(subscribe [::db/data-field :get-publications-node-graph])
        insti? @(subscribe [::mp/insti?])
        ]
    node-data)
  )

(comment 
  (def node-data @(subscribe [::db/data-field :get-publications-node-graph]))
  node-data

  )

(defn selected-comp []
  (let [selected (subscribe [::g/graph-field :selected])
        edge-data (subscribe [::db/data-field :get-publications-edge-graph])
        node-data (subscribe [::db/data-field :get-publications-node-graph])]
    (fn []
      (let [selected-ele (clojure.string/split @selected #"_")
            data (if (= 1 (count selected-ele)) @node-data @edge-data)
            id (if (= 1 (count selected-ele)) :get-publications-node-graph :get-publications-edge-graph)
            content (count data)]
        ^{:key content}
        [loading-content id
         [:span (str content)]]))))

(defn selected-info []
  (let [selected (subscribe [::g/graph-field :selected])]
    (fn []
      [:div
       [:div {:style {:display :flex :justify-content :space-between
                      :width "100%" :height "100%" #_#_:border-style :solid}}
        [:h3 {:style {:margin 0}} "INFO BOX"]
        [button/close-button
         {:on-click  #(dispatch [::g/set-graph-field [:info-open?] false])}]]
       [:h4 @selected]
       [selected-comp]])))

(comment
  (def config {"from_year" 2015
               "region_ids" ["dach"]
               "strict_boundary" true,
               "institution" true})
  (dispatch [::api/get-publications-node :graph "EPFL" config])
  (dispatch [::api/get-publications-edge :graph (clojure.string/split "Graz University of Technology_EPFL" #"_") config])
  (count @(subscribe [::db/data-field :get-publications-node-graph]))
  @(subscribe [::db/data-field :get-publications-edge-graph]) 
  )