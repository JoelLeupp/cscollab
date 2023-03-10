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
            [app.components.stack :refer (horizontal-stack)]
            [app.components.table :refer (basic-table)]
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
        author-map (frequency-counter node-data :collab_pid)
        csauthors @(subscribe [::data/csauthors])
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

(defn inst-info [node-data] 
  
  [:div
   [author-table node-data]])

(defn author-info [node-data]
  (count node-data))

(defn collab-info [edge-data insti?]
  (count edge-data)
  )

(comment
  (def node-data @(subscribe [::db/data-field :get-publications-node-graph]))
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

(defn selected-info []
  (let [selected (subscribe [::g/graph-field :selected])
        edge-data (subscribe [::db/data-field :get-publications-edge-graph])
        node-data (subscribe [::db/data-field :get-publications-node-graph])
        csauthors (subscribe [::data/csauthors])
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