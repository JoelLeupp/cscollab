(ns app.cscollab.view.visualization.analytics.analytics-plots
  (:require [reagent.core :as reagent :refer [atom]]
            [app.cscollab.data :as data]
            [app.components.colors :refer [colors area-color sub-area-color]]
            [app.components.lists :refer [collapse]]
            [app.db :as db]
            [app.cscollab.panels.filter-panel :as filter-panel]
            [app.components.grid :as grid]
            [app.cscollab.view.visualization.selected-info :as selected-info]
            [app.components.table :as table]
            [app.common.container :refer (analytics-container)]
            [app.components.stack :refer (horizontal-stack)]
            ["@mui/material/MenuItem" :default mui-menu-item]
            ["@mui/material/ListSubheader" :default mui-list-subheader]
            [app.components.loading :refer (loading-content)]
            [app.cscollab.common :as common]
            [app.cscollab.panels.map-panel :as mp]
            [app.components.feedback :as feedback]
            [app.cscollab.api :as api]
            [app.components.tabs :as tabs]
            [app.common.plotly :as plotly]
            [app.components.lists :as lists]
            [app.components.button :as button]
            [goog.string :as gstring]
            [app.components.inputs :as i]
            [goog.string.format]
            [re-frame.core :refer
             (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
            [app.util :as util]))

(defn select-perspective [insti?]
  (fn []
    [i/autocomplete
     {:id :select-perspective
      :label "select a view"
      :style {:width 400}
      :options
      (concat
       [{:value :publications :label "Publications by Area"}
        {:value :year :label "Publications by Year"}]
       (when insti?
         [{:value :authors :label "List of Affiliated Authors"}])
       [{:value :author-collab :label "Collaborations with Authors"}
        {:value :institutions :label "Collaborations with Institutions"}
        {:value :countries :label "Collaborations with Countries"}])}]))


(defn select-overview-perspective []
  (fn []
    [i/autocomplete
     {:id :select-overview-perspective
      :label "select a view"
      :style {:width 400}
      :options
      [{:value :institutions :label "publications by institutions"}
       {:value :author :label "publications by authors"}
       {:value :countries :label "publications by countries"} 
       {:value :area :label "publications by area"}
       {:value :sub-area :label "publications by sub areas"}
       {:value :author-institution :label "authors by institutions"}
       {:value :author-countries :label "authors by country"}]}]))

(defn select-institution []
  (let [collab (subscribe [::db/data-field :get-filtered-collab])]
    (fn []
      (let [institutions (clojure.set/union (set (map :b_inst @collab)) (set (map :a_inst @collab)))
            options (sort-by :label (map #(identity {:value % :label %}) institutions))]
        [i/autocomplete
         {:id :select-institution
          :keywordize-values false
          :label "chose an institution"
          :style {:width 400}
          :options options}]))))

(defn select-author []
  (let [collab (subscribe [::db/data-field :get-filtered-collab])
        csauthors (subscribe [::db/data-field :get-csauthors])]
    (fn []
      (let [authors (clojure.set/union (set (map :a_pid @collab)) (set (map :b_inst @collab)))
            pid->name (zipmap (map :pid @csauthors) (map :name @csauthors))
            options (sort-by :label (map #(identity {:value % :label (get pid->name % %)}) authors))]
        [i/autocomplete
         {:id :select-author
          :keywordize-values false
          :label "chose an author"
          :style {:width 400}
          :options options}]))))

(defn publication-plot [data]
  [selected-info/publication-plot
   {:node-data data
    :style {:width "100%"}
    :layout {:legend {:y 1.1 :x -0.5}}
    :box-args {:height "60vh" :min-width 700 :width "60%" :overflow :auto :margin-top 2}}])

(defn country-plot [node-data key author-count?]
  (let [selected-area (subscribe [::db/user-input-field :area-selection])
        tab-view (subscribe [::db/ui-states-field [:tabs :analytics-tabs]])
        region-mapping @(subscribe [::db/data-field :get-region-mapping])
        country-mapping (zipmap (map :country-id region-mapping) (map :country-name region-mapping))]
    (fn []
      (let [color (when
                   (and (= @tab-view :overview) 
                        (not (or (nil? @selected-area) (= :all (keyword @selected-area)))))
                    (if (namespace (keyword @selected-area))
                      ((keyword (name (keyword @selected-area))) sub-area-color)
                      ((keyword @selected-area) area-color)))]
        [selected-info/general-collab-plot
         {:node-data node-data
          :key key 
          :color-by color
          :author-count? author-count?
          :layout {:margin  {:l 300}}
          :style {:width "100%"}
          :name "collaborations by country"
          :box-args {:height "60vh" :min-width 700 :width "60%" :overflow :auto :margin-top 2}
          :mapping country-mapping}]))))

(defn year-plot [node-data]
  [selected-info/general-collab-plot
   {:node-data node-data
    :key :year
    :style {:width "100%"}
    :name "collaborations by year" 
    :box-args {:height "60vh" :min-width 700 :width "60%" :overflow :auto :margin-top 2}
    :layout {:yaxis {:tickformat "%Y"
                     :autorange nil
                     :type :date}}}])

(defn inst-plot [node-data]
  [selected-info/general-collab-plot
   {:node-data node-data
    :key :collab_inst
    :style {:width "100%"}
    :box-args {:height "60vh" :min-width 700 :width "60%" :overflow :auto :margin-top 2}
    :name "collaborations by institution"}])

(defn author-table [node-data]
  [selected-info/author-table
   {:node-data node-data
    :paper-args {:min-width 700 :width "60%" :justify-content :flex-start :margin-top 2 :margin nil}
    :container-args {:max-height "60vh"}}])

(defn with-author-plot [node-data key]
  (let [selected-area (subscribe [::db/user-input-field :area-selection])
        tab-view (subscribe [::db/ui-states-field [:tabs :analytics-tabs]])
        csauthors @(subscribe [::db/data-field :get-csauthors])
        pid->name (zipmap (map :pid csauthors) (map :name csauthors))]
    (fn []
      (let [color (when-not (and
                             (= @tab-view :overview)
                             (or (nil? @selected-area) (= :all (keyword @selected-area))))
                    (if (namespace (keyword @selected-area))
                      ((keyword (name (keyword @selected-area))) sub-area-color)
                      ((keyword @selected-area) area-color)))]
        
        [selected-info/general-collab-plot
         {:node-data node-data
          :key key
          :color-by color
          :layout {:margin  {:l 300}}
          :style {:width "100%"}
          :box-args {:height "60vh" :min-width 700 :width "60%" :overflow :auto :margin-top 2}
          :name "collaborations by institution"
          :mapping pid->name}]))))



(defn inst-overview-plot [collab-data author-count?]
  (let [selected-area (subscribe [::db/user-input-field :area-selection])
        tab-view (subscribe [::db/ui-states-field [:tabs :analytics-tabs]])]
    (fn []
      (let [color (when-not (and 
                             (= @tab-view :overview)
                             (or (nil? @selected-area) (= :all (keyword @selected-area))))
                    (if (namespace (keyword @selected-area))
                      ((keyword (name (keyword @selected-area))) sub-area-color)
                      ((keyword @selected-area) area-color)))]
        [selected-info/general-collab-plot
         {:node-data collab-data
          :color-by color
          :layout {:margin  {:l 300}}
          :author-count? author-count?
          :key [:a_inst :b_inst]
          :style {:width "100%"}
          :box-args {:height "60vh" :min-width 700 :width "60%" :overflow :auto :margin-top 2}
          :name "collaborations by institution"}]))))

(defn area-plot [collab-data area?]
  (let [area-mapping (subscribe [::data/area-mapping])]
    (fn []
      (let [area-names
            (vec
             (set (map #(select-keys % [:area-id :area-label :sub-area-id :sub-area-label]) @area-mapping)))
            sub-area-map
            (zipmap (map :sub-area-id area-names) area-names)
            mapping 
            (zipmap (map (if area? :area-id :sub-area-id) area-names) 
                    (map (if area? :area-label :sub-area-label) area-names))
            data (mapv #(assoc % :field
                               (get-in sub-area-map [(:rec_sub_area %) (if area? :area-id :sub-area-id)]))
                       collab-data)]
        [selected-info/general-collab-plot
         {:node-data data
          :key :field
          :layout {:margin  {:l 300}}
          :mapping mapping
          :color-by (if area? :area :subarea)
          :style {:width "100%"}
          :box-args {:height "60vh" :min-width 700 :width "60%" :overflow :auto :margin-top 2}
          :name (str "collaborations by " (if area? "area" "sub area"))}]))))

(defn area-selection []
  (let [area-mapping (subscribe [::data/area-mapping])
        selected-areas (subscribe [::filter-panel/selected-areas])
        selected-sub-areas (subscribe [::filter-panel/selected-sub-areas])]
    (fn []
      (let [area-ids [:ai :systems :theory :interdiscip]
            nested-area 
            (util/factor-out-key
             (mapv
              #(into
                {}
                [{:id
                  (util/s->id (-> % first first))
                  :label
                  (-> % first second)}
                 {:sub-areas
                  (sort-by
                   :label
                   (mapv
                    (fn [[k v]]
                      (into
                       {}
                       [{:id (util/s->id (first k)) :label (second k)}]))
                    (group-by (juxt :sub-area-id :sub-area-label) (second %))))}])
              (group-by (juxt :area-id :area-label) @area-mapping)) :id)]
        [i/select {:id :area-selection
                   :label-id :area-selection
                   :label "research field"
                   :keywordize-values false
                   :form-args {:style {:width 400}}
                   :select-args {:MenuProps {:PaperProps {:style {:max-height 400}}}}
                   :option
                   (list [:> mui-menu-item {:value  "all"} "All"] 
                         [:> mui-list-subheader {:sx {:font-size 18}} "Areas"]
                         (for [id area-ids]
                           (when (contains? @selected-areas id)
                             [:> mui-menu-item {:value id} (get-in nested-area [id :label])]))
                         [:> mui-list-subheader {:sx {:font-size 18}} "Sub Areas"]
                         (for [id area-ids]
                           (when (contains? @selected-areas id)
                             (list [:> mui-list-subheader {:sx {:font-size 16}} (get-in nested-area [id :label])]
                                   (for [sub-area (get-in nested-area [id :sub-areas])]
                                     (when contains? @selected-sub-areas
                                           [:> mui-menu-item {:value (str (name id) "/" (name (:id sub-area)))} (:label sub-area)]))))))}]))))
(defn overview []
  (let [collab (subscribe [::db/data-field :get-filtered-collab])
        perspecitve (subscribe [::db/user-input-field :select-overview-perspective])
        selected-area (subscribe [::db/user-input-field :area-selection])
        area-mapping (subscribe [::data/area-mapping])]
    (fn []
      (let [area-names
            (vec
             (set (map #(select-keys % [:area-id :area-label :sub-area-id :sub-area-label]) @area-mapping)))
            sub-area-map
            (zipmap (map :sub-area-id area-names) area-names)
            data (mapv #(assoc % :rec_area
                               (get-in sub-area-map [(:rec_sub_area %) :area-id])) @collab)
            collab-filtered (filter #(if (or (nil? @selected-area) (= :all (keyword @selected-area)))
                                       true
                                       (if (namespace (keyword @selected-area))
                                         (= (name (keyword @selected-area)) (:rec_sub_area %))
                                         (=  @selected-area (:rec_area %))))
                                    data)]
        ^{:key [@collab @perspecitve @selected-area]}
        [:div 
         [:div {:style {:display :flex :justify-content :flex-start :gap 40}}
          [select-overview-perspective]
          (case @perspecitve
            :institutions [area-selection]
            :author [area-selection]
            :countries [area-selection]
            :author-institution [area-selection]
            :author-countries [area-selection]
            nil)] 
         [loading-content :get-filtered-collab
          (when @collab
            (case @perspecitve
              :institutions [inst-overview-plot collab-filtered false]
              :author [with-author-plot collab-filtered [:a_pid :b_pid]]
              :countries [country-plot collab-filtered [:a_country :b_country] false]
              :author-institution [inst-overview-plot collab-filtered true]
              :author-countries [country-plot collab-filtered [:a_country :b_country] true]
              :area [area-plot @collab true]
              :sub-area [area-plot @collab false]
              [inst-overview-plot collab-filtered false]))]]))))

(defn institution-view []
  (let [selected-inst (subscribe [::db/user-input-field :select-institution])
        inst-data (subscribe [::db/data-field :get-publications-node-inst])
        perspecitve (subscribe [::db/user-input-field :select-perspective])]
    (add-watch selected-inst ::select-inst
               (fn [_ _ _ selected]
                 (let [config (assoc @(subscribe [::common/filter-config]) "institution" true)]
                   (dispatch [::api/get-publications-node :inst selected config]))))
    (fn []
      [:div
       [:div {:style {:display :flex :justify-content :flex-start :gap 40}}
        [select-institution]
        [select-perspective true]]
       ^{:key [@selected-inst @inst-data @perspecitve]}
       [loading-content :get-publications-node-author
        (when @inst-data
          (case @perspecitve
            :publications [publication-plot @inst-data]
            :authors [author-table @inst-data]
            :institutions [inst-plot @inst-data]
            :countries [country-plot @inst-data :collab_country] 
            :year [year-plot @inst-data]
            :author-collab [with-author-plot @inst-data :collab_pid]
            [publication-plot @inst-data]) 
          )]])))

(defn author-view []
  (let [selected-author (subscribe [::db/user-input-field :select-author])
        author-data (subscribe [::db/data-field :get-publications-node-author])
        perspecitve (subscribe [::db/user-input-field :select-perspective])]
    (add-watch selected-author ::select-author
               (fn [_ _ _ selected]
                 (let [config (assoc @(subscribe [::common/filter-config]) "institution" false)]
                   (dispatch [::api/get-publications-node :author selected config]))))
    (fn []
      [:div
       [:div {:style {:display :flex :justify-content :flex-start :gap 40}}
        [select-author]
        [select-perspective false]] 
       ^{:key [@selected-author @author-data @perspecitve]}
       [loading-content :get-publications-node-author
        (when @author-data
          (case @perspecitve
            :publications [publication-plot @author-data]
            :institutions [inst-plot @author-data]
            :countries [country-plot @author-data :collab_country]
            :year [year-plot @author-data]
            :author-collab [with-author-plot @author-data :collab_pid]
            [publication-plot @author-data]))]])))


(defn select-timeline-perspective []
  (fn []
    [i/autocomplete
     {:id :select-timeline-perspective
      :label "select a view"
      :style {:width 400}
      :options
      [{:value :total-publications :label "Total Publications"}
       {:value :publications-by-area :label "Publications by Area"}
       {:value :publications-by-subarea :label "Publications by Sub Area"}]}]))

(defn line-trace [{:keys [x y name color config] :or {color (:main colors)}}]
  (util/deep-merge
   {:name name
    :x x
    :y y
    :type :scatter
    :mode :lines
    :line {:color color :width 3}
    :hovertemplate "%{x}: %{y:.0f}"}
   config))


(defn line-plots
  [{:keys [plot-data box-args style layout]}] 
  (fn []
    [plotly/plot
     {:box-args (util/deep-merge
                 {:height "60vh" :width "100%" :overflow :auto :margin-top 0}
                 box-args)
      :style (util/deep-merge {:width "100%" :height "100%"} style) 
      :layout (util/deep-merge
               {:hovermode :closest #_"x"
                :margin  {:pad 0 :t 50 :b 0 :l 80 :r 80}
                :legend {:orientation :h :y -0.1 :font {:size 12}} 
                :showlegend true
                :yaxis {:showgrid true :zeroline false :rangemode :tozero :ticksuffix "  "}
                :xaxis {:showgrid false :showline true :ticklen 6 :linewith 2 :type :date :tickformat "%Y"}}
               layout)
      :data plot-data}]))

(defn all-publications []
  (let [collab (subscribe [::db/data-field :get-filtered-collab])]
    (fn []
      (let [data
            (reverse
             (sort-by
              :year
              (map
               (fn [[grp-key values]]
                 {:year grp-key
                  :count (count (set (map :rec_id values)))})
               (group-by :year @collab))))
            x (mapv :year data)
            y (mapv :count data)
            sum (reduce + y)
            plot-data [(line-trace {:x x :y y 
                                    :name (str "Yearly Publications")})]]
        [line-plots {:plot-data plot-data}]))))

(defn publications-by-area [area?]
  (let [collab (subscribe [::db/data-field :get-filtered-collab])
        area-mapping (subscribe [::data/area-mapping])]
    (fn []
      (let [area-names
            (vec
             (set (map #(select-keys % [:area-id :area-label :sub-area-id :sub-area-label]) @area-mapping)))
            sub-area-map
            (zipmap (map :sub-area-id area-names) area-names)
            mapping
            (zipmap (map (if area? :area-id :sub-area-id) area-names)
                    (map (if area? :area-label :sub-area-label) area-names))
            collab (mapv #(assoc % :field
                                 (get-in sub-area-map [(:rec_sub_area %) (if area? :area-id :sub-area-id)]))
                         @collab)
            color-map (if area? area-color sub-area-color)
            data (map
                  (fn [[k v]]
                    {:key k
                     :data
                     (reverse
                      (sort-by
                       :year
                       (map
                        (fn [[grp-key values]]
                          {:year grp-key
                           :count (count (set (map :rec_id values)))})
                        (group-by :year v))))})
                  (group-by :field  collab))
            data-plot (mapv #(line-trace {:x (mapv :year (:data %))
                                          :y (mapv :count (:data %))
                                          :name (get mapping (:key %))
                                          :color (get color-map (keyword (:key %)))}) data)]
        [line-plots {:plot-data (sort-by :name data-plot)
                     :layout {:legend {:font {:size 12}}}}]))))

(defn timeline-view []
  (let [perspecitve (subscribe [::db/user-input-field :select-timeline-perspective])
        collab (subscribe [::db/data-field :get-filtered-collab])]
    (fn []
      [:div
       [:div {:style {:display :flex :justify-content :flex-start :gap 40}}
        [select-timeline-perspective]]
       ^{:key [@collab @perspecitve]}
       [loading-content :get-filtered-collab
        (when @collab
          (case @perspecitve
            :total-publications [all-publications]
            :publications-by-area [publications-by-area true]
            :publications-by-subarea [publications-by-area false]
            [all-publications]))]])))


