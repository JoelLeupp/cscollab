(ns app.cscollab.view.visualization.graph.graph-view
  (:require [reagent.core :as reagent :refer [atom]]
            [app.cscollab.transformer :as tf]
            [app.cscollab.data :as data]
            [app.cscollab.panels.filter-panel :as filter-panel]
            [app.components.colors :refer [colors area-color sub-area-color]]
            [app.components.lists :refer [collapse]]
            [cljs-bean.core :refer [bean ->clj ->js]]
            [app.db :as db]
            [app.components.feedback :as feedback]
            [app.cscollab.view.visualization.graph.graph :as g]
            [app.cscollab.panels.map-panel :as mp]
            [app.components.button :as button]
            [reagent-mui.material.paper :refer [paper]] 
            [leaflet :as L]
            [app.cscollab.common :as common]
            [app.cscollab.api :as api]
            [app.cscollab.view.visualization.selected-info :refer (selected-info-graph)]
            [app.common.container :refer (viz-container)]
            [re-frame.core :refer
             (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
            [app.util :as util]))

(declare gen-edges)
(declare gen-nodes)

(defn gen-elements []
  (let [weighted-collab @(subscribe [::db/data-field :get-weighted-collab])
        csauthors @(subscribe [::db/data-field :get-csauthors])
        frequency @(subscribe [::db/data-field :get-frequency])
        node-position @(subscribe [::db/data-field :get-node-position])
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
    (if (and weighted-collab csauthors geo-mapping frequency node-position) 
      (vec
       (concat
        (gen-edges weighted-collab geo-mapping)
        (gen-nodes weighted-collab geo-mapping node-position frequency)))
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

(defn linear-scale [min-w max-w w max-scale]
  "scale for weights between 1 and max-scale based on min and max of all weights"
  (let [slope (/ (- max-scale 1) (- max-w min-w))
        shift (- 1 (* min-w slope))]
    (+ shift (* slope w))))

(defn color-node [id]
  (let [color-by @(subscribe [::mp/color-by])
        frequency @(subscribe [::db/data-field :get-frequency])
        analytics @(subscribe [::db/data-field :get-analytics])]
    (if (or (= color-by :degree-centrality) (= color-by :eigenvector-centrality))
      (let [centrality-key (if (= color-by :degree-centrality) :degree_centrality :eigenvector_centrality)
            centrality-data (get-in analytics [:centralities centrality-key])
            factor (/ 1 (:value (first centrality-data)))
            top-centralities (subvec centrality-data 0 (min 20 (count centrality-data)))
            mapping (zipmap
                     (map :id top-centralities)
                     (map #(- 1 (* 0.025 %)) (range 20))
                     #_(map #(* factor 1.5 (:value %)) top-centralities))
            value (get mapping id)]
        {:bg (if value (:second colors) (:main colors))
         :opacity (if value value 1)})
      {:bg (case color-by
             :area (get area-color (keyword (get-in frequency [id "area" "top"])))
             :subarea (get sub-area-color (keyword (get-in frequency [id "subarea" "top"])))
             (:main colors))
       :opacity 1})))


(defn gen-nodes [weighted-collab geo-mapping node-position frequency]
  (let [nodes (vec (clojure.set/union
                    (set (map :node/m weighted-collab))
                    (set (map :node/n weighted-collab))))
        color-by @(subscribe [::mp/color-by])
        insti? @(subscribe [::mp/insti?])
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
        max-weight (apply max weights)]
    (mapv #(hash-map
            :style {:width (* 20 (linear-scale min-weight max-weight (get node->weight %) 3) #_(percentil-scale weights (get node->weight %)))
                    :height (* 20 (linear-scale min-weight max-weight (get node->weight %) 3) #_(percentil-scale weights (get node->weight %)))}
            :data (merge {:id % 
                          #_(case color-by
                              :area (get area-color (keyword (get-in frequency [% "area" "top"])))
                              :subarea (get sub-area-color (keyword (get-in frequency [% "subarea" "top"])))
                              (:main colors))
                          #_#_:label (get-in geo-mapping [% :name])}
                         (color-node %))
            :position {:x (* (if insti? 100 30) (get-in node-position [% "x"]))
                       :y (* (if insti? 100 30) (get-in node-position [% "y"]))})
          nodes)))

(defn gen-edges [weighted-collab geo-mapping] 
  (let [weights (mapv :weight weighted-collab)
        min-weight (apply min weights)
        max-weight (apply max weights)]
    (mapv
     #(let [node-m (get geo-mapping (:node/m %))
            node-n (get geo-mapping (:node/n %))]
        (when (and node-m node-n)
          (hash-map
           :data {:id (str (:id node-m) "_" (:id node-n)) #_[(:id node-m) (:id node-n)]
                  :source (:id node-m)
                  :target (:id node-n)}
           :style {:width (linear-scale min-weight max-weight (:weight %) 8)})))
     weighted-collab)))


(defn graph-comp [] 
  (let [elements (subscribe [::g/elements]) 
        analytics (subscribe [::db/data-field :get-analytics])] 
    (fn [] 
      ^{:key [@elements @analytics]}
      [g/graph {:on-click (fn [e]
                            (dispatch [::g/set-graph-field [:info-open?] true])
                            (dispatch [::g/set-graph-field :selected (.. e -target data -id)]))
                :elements (or @elements [])
                :layout {:name :preset #_:grid}
                :stylesheet  [{:selector "node"
                               :style {:background-color (fn [ele]
                                                           (get (->clj (.. ele data)) :bg (:main colors)))
                                       :background-opacity (fn [ele]
                                                             (get (->clj (.. ele data)) :opacity 1))
                                       :shape (fn [ele] (or (.. ele data -shape) "ellipse"))}}
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
                              #_{:selector :edge
                               :style {:line-color (:main colors)}}]
                :style {:width "100%" :hight "100%" :background-color "white"}}])))


(defn get-all-graph-data []
  (let [config @(subscribe [::common/filter-config])
        color-by @(subscribe [::mp/color-by]) 
        sub-areas? (if (= color-by :subarea) true false)]
    (dispatch [::api/get-node-position config sub-areas?])
    (dispatch [::api/get-weighted-collab config])
    (dispatch [::api/get-frequency config])
    (dispatch [::api/get-filtered-collab config])
    (dispatch [::api/get-analytics config 200])))

(defonce reset (atom 0))

(defn graph-update [] 
  (swap! reset inc)
  (dispatch [::g/set-graph-field :selected nil])
  (dispatch [::g/set-graph-field [:info-open?] false])
  (get-all-graph-data))

(defn graph-view []
  (let [insti? (subscribe [::mp/insti?])
        #_#_node-position (subscribe [::db/data-field :get-node-position])
        color-by (subscribe [::mp/color-by])
        loading? (subscribe [::api/graph-data-loading?])]
    (add-watch (subscribe [::g/graph-field :selected]) ::select-connected
               (fn [_ _ _ selected]
                 (when selected
                   (let [cy (subscribe [::g/cy])
                         config @(subscribe [::common/filter-config])
                         e (.getElementById ^js @cy selected)
                         selected-ele (clojure.string/split selected #"_")
                         ele (if (= 1 (count selected-ele)) (first selected-ele) selected-ele)]
                     (if (string? ele)
                       (dispatch [::api/get-publications-node :graph ele config])
                       (dispatch [::api/get-publications-edge :graph ele config]))
                     (if (contains? (->clj (.data e)) :source)
                       (.select (.connectedNodes e))
                       (.select (.neighborhood e)))))))
    (add-watch loading? ::graph-data-loading
               (fn [_ _ _ data-loading?]
                 (when (= :graph @(subscribe [::db/ui-states-field [:tabs :viz-view]]))
                   (if data-loading?
                     (dispatch [::feedback/open :graph-data-loading])
                     (do
                       (dispatch [::feedback/close :graph-data-loading])
                       (dispatch [::g/set-graph-field [:elements] (gen-elements)]))))))
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
         :legend-bg-color :transparent
         :color-by @color-by
         :title (if @insti? "Affiliation Graph" "Collaboration Graph")
         :content [graph-comp] #_[:div {:style {:margin 0 :padding 0 :width "100%" :height "100%" :text-align :center}}]
         :info-component [selected-info-graph]
         :info-open? (subscribe [::g/info-open?])
         :update-event #(graph-update)
         #_#(dispatch [::g/set-graph-field [:elements] (gen-elements)])}]])))


  