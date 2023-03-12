(ns app.cscollab.selected-info
  (:require [reagent.core :as reagent :refer [atom]]
            [app.cscollab.transformer :as tf]
            [app.cscollab.data :as data]
            [app.cscollab.filter-panel :as filter-panel]
            [app.components.colors :refer [colors area-color sub-area-color]]
            [app.components.lists :refer [collapse]]
            [cljs-bean.core :refer [bean ->clj ->js]]
            [app.db :as db]
            [app.common.leaflet :as ll]
            [app.components.feedback :as feedback]
            [app.common.graph :as g]
            [app.cscollab.map-panel :as mp]
            [app.components.button :as button]
            [reagent-mui.material.paper :refer [paper]] 
            [app.cscollab.common :as common]
            [app.components.stack :refer (horizontal-stack)]
            [app.components.table :refer (basic-table)]
            [app.common.plotly :as plotly]
            [app.cscollab.api :as api] 
            [app.components.loading :refer (loading-content)]
            [re-frame.core :refer
             (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
            [app.util :as util]))

(defn frequency-counter [data key]
  (reverse
   (sort-by
    :count
    (map
     (fn [[grp-key values]]
       {:key grp-key
        :count (count (set (map :rec_id values)))})
     (group-by key data)))))


(defn dblp-author-page [pid]
  (str "https://dblp.org/pid/" pid ".html"))



(defn author-table [node-data]
  (let [collab-count (count (set (map :rec_id node-data)))
        author-map (frequency-counter node-data :pid)
        csauthors @(subscribe [::db/data-field :get-csauthors])
        pid->name (zipmap (map :pid csauthors) (map :name csauthors))
        header [{:id :author
                 :label [:b (str "Authors (" (count author-map) ")")]}
                {:id :count
                 :label [:b (str "Publications (" collab-count ")")] :align :right}]
        author-item (fn [pid]
                      [horizontal-stack
                       {:stack-args {:spacing 2}
                        :items [[:span (get pid->name pid)]
                                [:a {:href (dblp-author-page pid)}
                                 [:img {:src "img/dblp.png" :target "_blank"
                                        :width 10 :height 10 :padding 10}]]
                                [:div {:style {:margin-top 1}}
                                 [:img {:src "img/scholar-favicon.ico" :target "_blank"
                                        :width 10 :height 10}]]]}])
        author-count (map #(assoc % :author (author-item (:key %))) author-map)]
    (fn []
      [basic-table
       {:header header
        :body author-count
        :paper-args {:sx {:width "100%" :display :flex :justify-content :center :margin :auto} :elevation 0}
        :container-args {:sx {:max-height "22vh"}}
        :table-args {:sticky-header true :size :small}}])))

(defn get-plot-data [data]
  (let [indexed-data (map #(assoc %1 :tickval %2) data (range 0 (count data)))
        grouped-data (group-by (juxt :area-id :area-label) indexed-data)]
    (vec
     (for [[[area-id area-label] vals] grouped-data]
       {:x (mapv :count vals)
        :y (mapv :tickval vals)
        :name (str area-label " (" (reduce + (map :count vals)) ")")
        :type :bar
        :orientation :h
        :hoverinfo "none"
        :textposition :outside
        :text (mapv #(str (:count %)) vals)
        :transforms [{:type :sort :target :x :order :descending}]
        :marker {:color (get area-color (keyword area-id))}}))))

(defn publication-plot [node-data title?]
  (let [area-mapping (subscribe [::data/area-mapping])
        full-screen? (subscribe [:app.common.container/full-screen? :map-container])]
    (fn []
      (let
       [area-names
        (vec
         (set (map #(select-keys % [:area-id :area-label :sub-area-id :sub-area-label]) @area-mapping)))
        sub-area-map
        (zipmap (map :sub-area-id area-names) area-names)
        sub-area-count (frequency-counter node-data :rec_sub_area)
        area-data
        (map #(let [sub-area-info (get sub-area-map (:key %))]
                (merge sub-area-info %)) sub-area-count)
        plot-data (get-plot-data area-data)] 
        [plotly/plot
         {:box-args {:height (if @full-screen? (if title? "70vh" "60vh")  (if title? "50vh" "36vh"))  :width 460 :overflow :auto :margin-top 2}
          :style {:width 440 :height (max 300 (+ 150 (* 45 (count area-data)) (when title? 100)))}
          :layout {:margin  {:pad 10 :t (if title? 80 0) :b 30 :l 200 :r 5}
                   :bargap 0.2
                   :annotations 
                   (when title?
                     [{:xref :paper :yref :paper :xanchor :left :yanchor :top :x 0 :y 1.2
                       :xshift -175 :yshift 10 :text "<b>Publications by Area</b>"
                       :align :left :showarrow false :font {:size 18}}])
                   #_#_:title "Publications per Area"
                   :legend {:y 1.1 :x -1
                            :orientation :h}
                   :xaxis {:range [0 (+ 25 (apply max (map :count area-data)))]}
                   :yaxis {:autorange :reversed
                           :tickmode :array
                           :tickvals (vec (range 0 (count area-data)))
                           :ticktext (mapv #(util/wrap-line (:sub-area-label %) 30) area-data)}}
          :data plot-data}]))))

(defn inst-info [node-data] 
  [:div
   [author-table node-data]
   [publication-plot node-data false]])

(defn author-info [node-data]
  [publication-plot node-data false])

(defn collab-info [edge-data insti?]
  [publication-plot edge-data false]
  )

(comment
  (def node-data @(subscribe [::db/data-field :get-publications-node-graph]))
  (def node-data @(subscribe [::db/data-field :get-publications-node-map]))
  (first node-data)
  (def self-collab (filter #(= "ETH Zurich" (:collab_inst %)) node-data))
  (map #(hash-map :pid (:collab_pid %) :collab_pid (:pid %)) self-collab)

  (def edge-data @(subscribe [::db/data-field :get-publications-edge-map]))

  (def selected @(subscribe [::g/graph-field :selected]))
  (count (set (map :rec_id node-data)))
  (reverse
   (sort-by
    :count
    (map
     (fn [[grp-key values]]
       {:author grp-key
        :count (count (set (map :rec_id values)))})
     (group-by :collab_pid node-data))))
  (frequency-counter node-data :collab_inst)
  (frequency-counter node-data :rec_sub_area)
  (frequency-counter node-data :collab_pid)
  (frequency-counter node-data :collab_country)
  (reverse (sort-by :key (frequency-counter node-data :year)))
  )

(defn selected-info-graph []
  (let [selected (subscribe [::g/graph-field :selected])
        edge-data (subscribe [::db/data-field :get-publications-edge-graph])
        node-data (subscribe [::db/data-field :get-publications-node-graph])
        csauthors (subscribe [::db/data-field :get-csauthors])
        insti? (subscribe [::mp/insti?])]
    (fn []
      (let [selected-ele (clojure.string/split @selected #"_")
            node? (= 1 (count selected-ele))
            data (if node? @node-data @edge-data)
            id (if node? :get-publications-node-graph :get-publications-edge-graph)]
        [:div
         [:div {:style {:display :flex :justify-content :space-between
                        :width "100%" :height "100%" #_#_:border-style :solid}}
          (if node?
            (if @insti?
              [:h3 #_{:style {:margin-top 0}} (first selected-ele)]
              (let [{:keys [name institution]} (first (filter #(= (first selected-ele) (:pid %)) @csauthors))]
                [:div [:h3 {:style {:margin-bottom 0}} name] [:h4 {:style {:margin 0}} institution]]))
            [:h3 {:style {:margin 0}} "Collaboration"])
          [button/close-button
           {:on-click  #(dispatch [::g/set-graph-field [:info-open?] false])}]]
         (when-not node?
           [:h4
            (if @insti?
              (first selected-ele)
              (:name (first (filter #(= (first selected-ele) (:pid %)) @csauthors))))
            " And "
            (if @insti?
              (second selected-ele)
              (:name (first (filter #(= (second selected-ele) (:pid %)) @csauthors))))])
         ^{:key data}
         [loading-content id
          [:div
           (if node?
             (if @insti? [inst-info data] [author-info data])
             [collab-info data @insti?])]]]))))

(defn selected-info-map []
  (let [selected-shape (subscribe [::ll/selected-shape])
        edge-data (subscribe [::db/data-field :get-publications-edge-map])
        node-data (subscribe [::db/data-field :get-publications-node-map])
        csauthors (subscribe [::db/data-field :get-csauthors])
        insti? (subscribe [::mp/insti?])]
    (fn []
      (let [selected @selected-shape
            node? (string? selected)
            data (if node? @node-data @edge-data)
            id (if node? :get-publications-node-map :get-publications-edge-map)]
        [:div
         [:div {:style {:display :flex :justify-content :space-between
                        :width "100%" :height "100%" #_#_:border-style :solid}}
          (if node?
            (if @insti?
              [:h3 #_{:style {:margin-top 0}} selected]
              (let [{:keys [name institution]} (first (filter #(= selected (:pid %)) @csauthors))]
                [:div [:h3 {:style {:margin-bottom 0}} name] [:h4 {:style {:margin 0}} institution]]))
            [:h3 {:style {:margin 0}} "Collaboration"])
          [button/close-button
           {:on-click  #(dispatch [::g/set-graph-field [:info-open?] false])}]]
         (when-not node?
           [:h4
            (if @insti?
              (first selected)
              (:name (first (filter #(= (first selected) (:pid %)) @csauthors))))
            " And "
            (if @insti?
              (second selected)
              (:name (first (filter #(= (second selected) (:pid %)) @csauthors))))])
         ^{:key data}
         [loading-content id
          [:div
           (if node?
             (if @insti? [inst-info data] [author-info data])
             [collab-info data @insti?])]]]))))

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