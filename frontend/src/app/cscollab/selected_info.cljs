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
            [app.components.tabs :as tabs] 
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
     (if (vector? key) 
       (merge-with into 
                   (group-by (first key) data)
                   (group-by (second key) data))
       (group-by key data))))))


(defn dblp-author-page [pid]
  (str "https://dblp.org/pid/" pid ".html"))



(defn author-table [{:keys [node-data paper-args container-args]}]
  (let [full-screen? (subscribe [:app.common.container/full-screen? :map-container])
        collab-count (count (set (map :rec_id node-data)))
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
                                #_[:div {:style {:margin-top 1}}
                                 [:img {:src "img/scholar-favicon.ico" :target "_blank"
                                        :width 10 :height 10}]]]}])
        author-count (map #(assoc % :author (author-item (:key %))) author-map)]
    (fn []
      [basic-table
       {:header header
        :body author-count
        :paper-args {:sx (util/deep-merge 
                          {:width 460 :display :flex :justify-content :center :margin :auto} paper-args) :elevation 0}
        :container-args {:sx (util/deep-merge {:max-height (if @full-screen? "80vh" "55vh")} container-args)}
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

(defn publication-plot [{:keys [node-data box-args style layout]}] 
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
         {:box-args (util/deep-merge 
                     {:height (if @full-screen? "80vh" "55vh")  :width 460 :overflow :auto :margin-top 2}
                     box-args)
          :style (util/deep-merge {:width 440 :height (max 300 (+ 150 (* 45 (count area-data))))} style)
          :layout (util/deep-merge
                   {:margin  {:pad 10 :t 0 :b 30 :l 200 :r 5}
                    :bargap 0.2
                    #_#_:title "Publications per Area"
                    :legend {:y 1.1 :x -1
                             :orientation :h}
                    :xaxis {:range [0 (+ 25 (apply max (map :count area-data)))]}
                    :yaxis {:autorange :reversed
                            :tickmode :array
                            :tickvals (vec (range 0 (count area-data)))
                            :ticktext (mapv #(util/wrap-line (:sub-area-label %) 30) area-data)}}
                   layout)
          :data plot-data}]))))

(defn get-plot-data-general [data name color] 
  (identity
   [{:x (mapv :count data)
     :y (mapv :key data)
     :name name
     :type :bar
     :orientation :h
     :hoverinfo "none"
     :textposition :outside
     :text (mapv #(str (:count %)) data)
     :transforms [{:type :sort :target :x :order :descending}]
     :marker {:color color}}]))

(defn general-collab-plot [{:keys [node-data key name mapping box-args style layout color-by]}]
  (let [full-screen? (subscribe [:app.common.container/full-screen? :map-container])]
    (fn []
      (let
       [counter (frequency-counter node-data key)
        color (case color-by
                :area (mapv #(get area-color (keyword (:key %))) counter)
                :subarea (mapv #(get sub-area-color (keyword (:key %))) counter)
                (:main colors))
        data (map #(assoc % :key (get mapping (:key %) (:key %))) counter) 
        plot-data (get-plot-data-general data name color)
        max-count (apply max (map :count data))] 
        [plotly/plot
         {:box-args (util/deep-merge 
                     {:height (if @full-screen? "80vh" "55vh")  :width 460 :overflow :auto :margin-top 2} 
                     box-args)
          :style (util/deep-merge {:width 440 :height (max 300 (+ 150 (* 45 (count data))))} style)
          :layout (util/deep-merge
                   {:margin  {:pad 10 :t 0 :b 30 :l 200 :r 5}
                    :bargap 0.2
                    :showlegend false
                    :xaxis {:range [0 (+ (* 0.1 max-count) max-count)]}
                    :yaxis {:autorange :reversed
                            :tickmode :array}}
                   layout)
          :data plot-data}]))))

(defn country-plot [node-data]
  (let [region-mapping @(subscribe [::db/data-field :get-region-mapping])
        country-mapping (zipmap (map :country-id region-mapping) (map :country-name region-mapping))]
    [general-collab-plot
     {:node-data node-data
      :key :collab_country
      :name "collaborations by country"
      :mapping country-mapping}]))

(defn year-plot [node-data]
  [general-collab-plot
   {:node-data node-data
    :key :year
    :name "collaborations by year"
    :layout {:yaxis {:autorange nil}}}])

(defn inst-plot [node-data]
  [general-collab-plot 
   {:node-data node-data
    :key :collab_inst
    :name "collaborations by institution"}])

(defn with-author-plot [node-data]
  (let [csauthors @(subscribe [::db/data-field :get-csauthors])
        pid->name (zipmap (map :pid csauthors) (map :name csauthors))]
    [general-collab-plot 
     {:node-data node-data
      :key :collab_pid
      :name "collaborations by institution"
      :mapping pid->name}]))

(defn node-info [node-data insti?] 
  (let [tab-view (subscribe [::db/ui-states-field [:tabs :inst-info]])
        full-screen? (subscribe [:app.common.container/full-screen? :map-container])] 
    [:div 
     [tabs/sub-tab
      {:id :inst-info
       :tabs-args {:variant :scrollable :scrollButtons :auto}
       :box-args {:margin-bottom "5px" :border-bottom 0 :width 460}
       :choices (vec (concat
                      [{:label "publications" :value :publication}]
                      (when insti? [{:label "authors" :value :author}])
                      [{:label "institutions" :value :institution}
                       {:label "countries" :value :country}
                       {:label "year" :value :year}
                       {:label "author collab" :value :with-author}]))}] 
     (case @tab-view
       :publication [publication-plot {:node-data node-data}]
       :author [author-table {:node-data node-data}]
       :country [country-plot node-data]
       :institution [inst-plot node-data]
       :year [year-plot node-data]
       :with-author [with-author-plot node-data]
       [publication-plot {:node-data node-data}])]))

(defn author-info [node-data]
  [publication-plot {:node-data node-data}])

(defn collab-info [edge-data insti?] 
  [publication-plot {:node-data edge-data}]
  )

(defn info-content [data node?]
  (let [insti? (subscribe [::data/insti?])]
    (if node?
      [node-info data @insti?]
      [collab-info data @insti?])))

(comment
  (def node-data @(subscribe [::db/data-field :get-publications-node-map]))
  
  (def node-data @(subscribe [::db/data-field :get-publications-node-map]))
  (first node-data)
  (def mapping nil)
  (let [region-mapping @(subscribe [::db/data-field :get-region-mapping])]
    (zipmap (map :country-id region-mapping) (map :country-name region-mapping)))
  (frequency-counter node-data :collab_country)
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
        insti? (subscribe [::data/insti?])]
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
                [:div [:h3 {:style {:margin-bottom 0}} name] [:h4 {:style {:margin-top 0 :margin-bottom 10}} institution]]))
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
          [info-content data node?]]]))))

(defn selected-info-map []
  (let [selected-shape (subscribe [::ll/selected-shape])
        edge-data (subscribe [::db/data-field :get-publications-edge-map])
        node-data (subscribe [::db/data-field :get-publications-node-map])
        csauthors (subscribe [::db/data-field :get-csauthors])
        insti? (subscribe [::data/insti?])]
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
                [:div [:h3 {:style {:margin-bottom 0}} name] [:h4 {:style {:margin-top 0 :margin-bottom 20}} institution]]))
            [:h3 {:style {:margin 0}} "Collaboration"])
          [button/close-button
           {:on-click  #(dispatch [::ll/set-leaflet [:info-open?] false])}]]
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
          [info-content data node?]]]))))

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