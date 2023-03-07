(ns app.cscollab.graph-view
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
  (let [weighted-collab @(subscribe [::db/data-field :get-weighted-collab])
        csauthors @(subscribe [::db/data-field :csauthors])
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

#_(reg-sub
 ::elements 
 :<- [::db/data-field :csauthors]
 :<- [::db/data-field :get-frequency]
 :<- [::db/data-field :get-weighted-collab]
 :<- [::db/data-field :get-node-position]
 (fn [[csauthors frequency weighted-collab node-position]]
   (if (and csauthors frequency weighted-collab node-position)
     (let [geo-mapping-inst
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
       (vec
        (concat
         (gen-edges weighted-collab geo-mapping)
         (gen-nodes weighted-collab geo-mapping node-position frequency))))
     [])))

(comment 
  @(subscribe [::mp/color-by])
  @(subscribe [::db/data-field :get-frequency])
  @(subscribe [::db/data-field :get-weighted-collab])
  @(subscribe [::db/data-field :get-node-position])
  )

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
            :data {:id %
                   :bg (case color-by  
                         :area (get area-color (keyword (get-in frequency [% "area" "top"]))) 
                         :subarea (get sub-area-color (keyword (get-in frequency [% "subarea" "top"])))
                         (:main colors))
                   #_#_:label (get-in geo-mapping [% :name])}
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
  (let [elements (subscribe [::g/elements]) #_(subscribe [::elements])] 
    (fn [] 
      ^{:key @elements}
      [g/graph {:on-click (fn [e] 
                            (dispatch [::g/set-graph-field [:info-open?] true])
                            (dispatch [::g/set-graph-field :selected (.. e -target data -id)])
                            (js/console.log (.. e -target data -id)))
                :elements (or @elements [])
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
                              #_{:selector :edge
                               :style {:line-color (:main colors)}}]
                :style {:width "100%" :hight "100%" :background-color "white"}}])))

#_(reg-sub
 ::elements
 :<- [::db/data-field :get-node-position]
 (fn [node-position]
   (when node-position
     (dispatch [::g/set-graph-field [:elements] (gen-elements)]))))

(defn get-all-graph-data []
  (let [config @(subscribe [::common/filter-config])
        color-by @(subscribe [::mp/color-by]) 
        sub-areas? (if (= color-by :subarea) true false)]
    (dispatch [::api/get-node-position config sub-areas?])
    (dispatch [::api/get-weighted-collab config])
    (dispatch [::api/get-frequency config])))

(defn info-component []
  (let [selected (subscribe [::g/graph-field :selected])]
    (fn []
      [:div {:style {:display :flex :justify-content :space-between
                     :width "100%" :height "100%" #_#_:border-style :solid}}
       [:h3 {:style {:margin 0}} "INFO BOX"]
       [:h4 @selected]
       [button/close-button
        {:on-click  #(dispatch [::g/set-graph-field [:info-open?] false])}]])))

(defn graph-view []
  (let [#_#_insti? (subscribe [::mp/insti?])
        #_#_node-position (subscribe [::db/data-field :get-node-position])
        loading? (subscribe [::api/graph-data-loading?])
        reset (atom 0)]
    (add-watch (subscribe [::g/graph-field :selected]) ::select-connected
               (fn [_ _ _ selected]
                 (let [cy (subscribe [::g/cy])
                       e (.getElementById @cy selected)]
                   (if (contains? (->clj (.data e)) :source)
                     (.select (.connectedNodes e))
                     (.select (.neighborhood e))))))
    (add-watch loading? ::graph-data-loading
               (fn [_ _ _ data-loading?]
                 (if data-loading?
                   (dispatch [::feedback/open :graph-data-loading])
                   (do
                     (dispatch [::feedback/close :graph-data-loading])
                     (dispatch [::g/set-graph-field [:elements] (gen-elements)])))))
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
         :info-component [info-component]
         :info-open? (subscribe [::g/info-open?])
         :update-event #(do (swap! reset inc)
                            (dispatch [::g/set-graph-field :selected nil])
                            (dispatch [::g/set-graph-field [:info-open?] false])
                            (get-all-graph-data))
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
  (def cy (subscribe [::g/cy]))
  (g/init-cytoscape-fcose)
  (last elements)
  (def cy @(subscribe [::db/ui-states-field [:graph :fcose]]))
  (.on @cy "tap" (fn [e] (js/console.log (= (.-target e) @cy))))
  (def layout (.layout @cy #js {:name "fcose"}))
  (.run layout)
  (js/console.log (.elements cy))
  (def e (.getElementById @cy "EPFL"))
  (def e (.getElementById @cy "Graz University of Technology_EPFL"))
  (.select (.connectedNodes e))
  (.select e)
  (contains? (->clj (.data e)) :source)
  (.unselect (.elements cy))
  (.id e)
  (->clj (.jsons e))
  (.json e (clj->js {:selected true}))
  (js/console.log (.edges cy "[source = \"g\"]"))
  (->clj (.jsons (.edges cy "[source = \"RWTH Aachen\"]")))
  (->clj (.jsons (.nodes @cy)))
  (dispatch [::g/set-graph-field [:info-open?] true])
  (subscribe [::g/info-open?])
  (subscribe [::db/ui-states-field [:viz :open?]])
  (dispatch [::db/set-ui-states [:viz :open? true]])
  )
  