(ns app.cscollab.graph-view
  (:require [reagent.core :as reagent :refer [atom]]
            [app.cscollab.transformer :as tf]
            [app.cscollab.data :as data]
            [app.cscollab.filter-panel :as filter-panel]
            [app.components.colors :refer [colors]]
            [app.components.lists :refer [collapse]]
            [cljs-bean.core :refer [bean ->clj ->js]]
            [app.db :as db]
            [app.components.feedback :as feedback]
            [app.common.graph :as g]
            [app.cscollab.map-panel :as mp]
            [app.components.button :as button]
            [reagent-mui.material.paper :refer [paper]]
            [leaflet :as L]
            [app.cscollab.common :as common]
            [app.cscollab.api :as api]
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

(defn percentil-scale [weights w]
  "scale based on percentil rank between 1 and 2"
  (let [p (util/percentil weights w)
        bin (js/Math.ceil (* 10 p))]
    (condp < bin
      9 2
      8 1.8
      7 1.6
      5 1.4
      1 1.2
      1)))

(defn linear-scale [min-w max-w w]
  "scale for weights between 1 and 2 based on min and max of all weights"
  (let [slope (/ 2 (- max-w min-w))
        shift (- 1 (* min-w slope))]
    (+ shift (* slope w))))


(defn gen-nodes [weighted-collab geo-mapping]
  (let [nodes (vec (clojure.set/union
                    (set (map :node/m weighted-collab))
                    (set (map :node/n weighted-collab))))
        weights
        (map
         #(reduce
           +
           (map :weight (filter (fn [node]
                                  (or
                                   (= % (:node/m node))
                                   (= % (:node/n node)))) weighted-collab)))
         nodes)
        node->weight (zipmap nodes weights)
        min-weight (apply min weights)
        max-weight (apply max weights)
        node-position @(subscribe [::db/data-field :get-node-position])]
    (mapv #(hash-map
            :style {:width (* 20 (linear-scale min-weight max-weight (get node->weight %))#_(percentil-scale weights (get node->weight %)))
                    :height (* 20 (linear-scale min-weight max-weight (get node->weight %)) #_(percentil-scale weights (get node->weight %)))}
            :data
            {:id %
             #_#_:label (get-in geo-mapping [% :name])}
            :position {:x (* 100 (get-in node-position [% "x"]))
                       :y (* 100 (get-in node-position [% "y"]))})
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
        [g/graph {:on-click (fn [e] (js/console.log (.. e -target data -id)))
                  :elements @elements
                  :layout {:name :preset #_:grid}
                  :stylesheet  [{:selector "node"
                                 :style {:background-color (fn [ele]
                                                             (get (->clj (.. ele data)) :bg (:main colors)))
                                         :shape (fn [ele] (or (.. ele data -shape) "ellipse"))
                                         #_#_:width 1 #_"label"
                                         #_#_:height 1 #_"label"
                                         #_#_:padding 6}}
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

(reg-sub
 ::elements
 :<- [::db/data-field :get-node-position]
 (fn [node-position]
   (when node-position
     (dispatch [::g/set-graph-field [:elements] (gen-elements)]))))

(defn get-all-graph-data []
  (let [config @(subscribe [::common/filter-config])
        sub-areas? false]
    (do
      (dispatch [::api/get-node-position config sub-areas?])
      (dispatch [::get-weighted-collab config])
      (dispatch [::get-frequency config]))))

(defn graph-view []
  (let [insti? (subscribe [::mp/insti?])
        #_#_node-position (subscribe [::db/data-field :get-node-position])
        loading? (subscribe [::api/graph-data-loading?])
        reset (atom 0)]
    #_(add-watch (subscribe [::db/data-field :get-node-position]) ::node-position
                 (fn [_ _ _ node-position]
                   (dispatch [::g/set-graph-field [:elements] (gen-elements)])))
    (add-watch loading? ::graph-data-loading
               (fn [_ _ _ data-loading?]
                 (if data-loading?
                   (dispatch [::feedback/open :graph-data-loading])
                   (dispatch [::feedback/close :graph-data-loading]))))
    (get-all-graph-data)
    (fn []
      ^{:key [@reset @loading?]}
      [:div
       [feedback/feedback {:id :graph-data-loading
                           :anchor-origin {:vertical :top :horizontal :center}
                           :status :info
                           :auto-hide-duration nil
                           :message "Garaph data is loading, please wait."}]
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
         :update-event #(do (swap! reset inc)
                            (dispatch [::api/get-node-position @(subscribe [::common/filter-config]) false]))
         #_#(dispatch [::g/set-graph-field [:elements] (gen-elements)])}]])))

(comment
  (:errors @re-frame.db/app-db)
  (:loading @re-frame.db/app-db)
  (dispatch [::g/set-graph-field [:elements] (gen-elements)])
  @(subscribe [::db/data-field :get-node-position])
  (def get-node-position @(subscribe [::db/data-field :get-node-position]))
  (get-in get-node-position ["13/6920" "x"])
  (dispatch [::api/get-node-position @(subscribe [::common/filter-config]) false])
  (def elements @(subscribe [::g/elements]))
  (filter #(not (get-in % [:data :source])) elements)
  (dispatch [::feedback/open :loading-position])
  (subscribe [::feedback/open? :loading-position])
  elements
  (subvec elements 0 5)
  (def cy @(subscribe [::g/cy]))
  (g/init-cytoscape-fcose)
  (last elements)
  (def cy (subscribe [::db/ui-states-field [:graph :fcose]]))
  (.on @cy "tap" (fn [e] (js/console.log (= (.-target e) @cy))))
  (def layout (.layout @cy #js {:name "fcose"}))
  (.run layout)
  (js/console.log (.elements cy))
  (def e (.getElementById cy "EPFL"))
  (.select e)
  (.unselect (.elements cy))
  (.id e)
  (->clj (.jsons e))
  (.json e (clj->js {:selected true}))
  (js/console.log (.edges cy "[source = \"g\"]"))
  (->clj (.jsons (.edges cy "[source = \"RWTH Aachen\"]")))
  (->clj (.jsons (.nodes @cy)))
  )
  