(ns app.cscollab.graph-view
  (:require [reagent.core :as reagent :refer [atom]]
            [app.cscollab.transformer :as tf]
            [app.cscollab.data :as data]
            [app.components.colors :refer [colors]]
            [app.components.lists :refer [collapse]]
            [cljs-bean.core :refer [bean ->clj ->js]]
            [app.db :as db]
            [app.common.graph :as g]
            [app.cscollab.map-panel :as mp]
            [app.components.button :as button]
            [reagent-mui.material.paper :refer [paper]]
            [leaflet :as L]
            [app.common.container :refer (viz-container)]
            [re-frame.core :refer
             (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
            [app.util :as util]))


(def elements
  [{:data {:id :a :label "A"}}
   {:data {:id :b :label "B"}}
   {:data {:id :f :label "F"}}
   {:data {:id :h :label "H" :bg :red :shape :round-rectangle}}
   {:data {:id :g :label "G" :type :exit}}
   {:data {:source :a :target :b}}
   {:data {:source :b :target :f}}
   {:data {:source :f :target :b}}
   {:data {:source :a :target :h}}
   {:data {:source :g :target :a}}
   {:data {:source :g :target :h}}
   {:data {:source :h :target :f}}])

(defn graph-comp []
  (g/init-cytoscape-dagre)
  (fn []
    [g/graph {:on-click (fn [e] (js/console.log (.. e -target data)))
              :elements elements
              :layout {:name :grid}
              :stylesheet  [{:selector "node"
                             :style {:background-color (fn [ele]
                                                         (get (->clj (.. ele data)) :bg (:main colors)))
                                     :shape (fn [ele] (or (.. ele data -shape) "ellipse"))
                                     :width "label"
                                     :height "label"
                                     :padding 6}}
                            {:selector "node[type=\"exit\"]"
                             :style {:background-color :black}}
                            {:selector "node:selected"
                             :style {:background-color (:second colors)}}
                            {:selector "edge:selected"
                             :style {:line-color (:second colors)}}
                            {:selector "node[label]"
                             :style {:label "data(label)"
                                     :color :white
                                     :font-size 6
                                     :text-halign :center
                                     :text-valign :center}}
                            {:selector :edge
                             :style {:curve-style :bezier
                                     :target-arrow-shape :triangle
                                     :arrow-scale 0.5
                                     :width 1}}]
              :style {:width "100%" :hight "100%" :background-color "white"}}]))

(defn graph-view []
  (let [insti? (subscribe [::mp/insti?])]
    (fn []
      [viz-container
       {:id :graph-container
        :title "Collaboration Graph"
        :content [graph-comp] #_[:div {:style {:margin 0 :padding 0 :width "100%" :height "100%" :text-align :center}}]
        :info-component [:div {:style {:display :flex :justify-content :space-between
                                       :width "100%" :height "100%" :border-style :solid}}
                         [:h3 {:style {:margin 0}} "INFO BOX"]
                         [button/close-button
                          {:on-click  #(dispatch [::db/set-ui-states [:viz :open?] false])}]]
        :info-open? (subscribe [::db/ui-states-field [:viz :open?]])}])))