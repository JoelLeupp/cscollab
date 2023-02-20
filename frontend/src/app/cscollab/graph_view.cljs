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

(declare gen-edges)
(declare gen-nodes)

(defn gen-elements []
  (let [insti? @(subscribe [::mp/insti?])
        weighted-collab
        (tf/weighted-collab {:insti? insti?})
        csauthors
        @(subscribe [::data/csauthors])
        geo-mapping-inst
        (zipmap (map :institution csauthors)
                (map #(hash-map
                       :id (:institution %)
                       :name (:institution %)) csauthors)) 
        geo-mapping-author
        (zipmap (map :pid csauthors)
                (map #(hash-map
                       :id (:pid %)
                       :institution (:institution %)
                       :name (:name %)) csauthors))
        geo-mapping
        (merge geo-mapping-inst geo-mapping-author)]
    (if (and weighted-collab csauthors geo-mapping) 
      (vec
       (concat
        (gen-edges weighted-collab geo-mapping)
        (gen-nodes weighted-collab geo-mapping)))
      [])))


(defn gen-nodes [weighted-collab geo-mapping]
  (let [nodes (vec (clojure.set/union
                    (set (map :node/m weighted-collab))
                    (set (map :node/n weighted-collab))))]
    (mapv #(hash-map :data
                     {:id %
                      :label (get-in geo-mapping [% :name])})
          nodes)))

(defn gen-edges [weighted-collab geo-mapping] 
  (mapv
   #(let [node-m (get geo-mapping (:node/m %))
          node-n (get geo-mapping (:node/n %))]
      (when (and node-m node-n)
        (hash-map :data
                  {:id (str (:id node-m) "_" (:id node-n))#_[(:id node-m) (:id node-n)]
                   :source (:id node-m)
                   :target (:id node-n)})))
   weighted-collab))

(def elements-test
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
  (let [elements (subscribe [::g/elements])] 
    (fn []
      (when (empty? @elements)
        (dispatch [::g/set-graph-field [:elements] (gen-elements)])) 
      (when @elements
        ^{:key @elements}
        [g/graph {:on-click (fn [e] (js/console.log (.. e -target data)))
                  :elements @elements
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
                                 :style {:width 1}}]
                  :style {:width "100%" :hight "100%" :background-color "white"}}]))))

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
        :info-open? (subscribe [::db/ui-states-field [:viz :open?]])
        :update-event #(dispatch [::g/set-graph-field [:elements] (gen-elements)])}])))

(comment
  (def elements @(subscribe [::g/elements]))
  elements
  (subvec elements 0 5)
  (last elements)
  )