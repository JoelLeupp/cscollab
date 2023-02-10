(ns app.cscollab.filter-panel
  (:require
   [app.common.user-input :refer (input-panel)]
   [app.cscollab.data :as data]
   [app.db :as db]
   [app.util :as util]
   [app.components.grid :as grid]
   [re-frame.core :refer
    (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
   [app.components.lists :as lists] 
   [app.components.inputs :as i]))


;; AREA FILTER
(reg-sub
 ::area-checkbox-content
 :<- [::data/nested-area]
 (fn
   [nested-area]
   "generates the checkbox content structure"
   (when nested-area
     (let
      [sorted-nested-area
       (sort-by
        #(let [idx (.indexOf
                    (clj->js ["ai" "systems" "theory" "interdiscip"])
                    (clj->js (:id %)))]
           (if (= idx -1) ##Inf idx))
        nested-area)
       all-area-item
       {:id :all :label "All Areas" :style {:font-size 18 :font-weight :medium}}
       list-items
       (for [area sorted-nested-area]
         (merge
          {:id (util/s->id (:id area))
           :label (:label area)}
          (when (:sub-areas area)
            {:children
             (for [sub-area (:sub-areas area)]
               {:id (util/s->id (:id sub-area))
                :label (:label sub-area)})})))]
       (into [all-area-item] list-items)))))



(defn area-checkbox-list [id]
  (let
   [content (subscribe [::area-checkbox-content])]
    (fn []
      (when @content 
        [lists/checkbox-list
         {:id id
          :subheader "Select Computer Science Areas"
          :style {:subheader {:font-size 18}}
          :list-args {:dense false :sx {:max-width 500 :width "100%"}}
          :content @content}]))))

(defn area-filter [id]
  [area-checkbox-list id])


;; REGION FILTER
(reg-sub
 ::region-checkbox-content
 :<- [::data/nested-region]
 (fn
   [nested-region]
   "generates the checkbox content structure"
   (when nested-region
     (let
      [sorted-nested-region
       (sort-by
        #(let [idx
               (.indexOf
                (clj->js [:europe :dach :northamerica :southamerica :asia :australasia :africa])
                (clj->js (:id %)))]
           (if (= idx -1) ##Inf idx))
        nested-region)
       world-item
       {:id :all 
        :label (or (:label (first (filter #(= (:id %) :wd) sorted-nested-region))) "All")
        :style {:font-size 18 :font-weight :medium}}
       list-items
       (for [region (filter #(not (= (:id %) :wd)) sorted-nested-region)]
         (merge
          {:id  (:id region)
           :label (:label region)}
          (when (:countries region)
            {:children
             (for [country (:countries region)]
               {:id  (:id country)
                :label (:label country)})})))]
       (into [world-item] list-items)))))



(defn region-checkbox-list []
  (let
   [content (subscribe [::region-checkbox-content])]
    (fn []
      (when @content
        [lists/checkbox-list
         {:id :region-checkbox
          :subheader "Select Region and Countries"
          :style {:subheader {:font-size 18}}
          :list-args {:dense false :sx {:max-width 500 :width "100%"}}
          :namespace-id? false
          :content @content}]))))

(defn region-filter [] 
  [region-checkbox-list]
  )

(defn stric-boundary-filter []
  [:div
   [:div {:title "Only consider collaborations within the selected regions/countries"}
    [lists/sub-header {:subheader "Strict Country Restriction"}]]
   [i/switch
    {:id :strict-boundary
     :default true
     :label-on "Only consider collaborations within the selected regions"}]])



(defn year-filter []
  (let [id :selected-year
        selected-year (subscribe [::db/user-input-field id]) 
        year-span (subscribe [::data/collab-year-span])] 
    (fn [] 
      (when (= @selected-year nil)
        (dispatch [::db/set-user-input id @year-span]))
      (let [[min-year max-year] @year-span
            mark-years
            (concat
             [min-year]
             (filter #(= 0 (mod % 5)) (range (+ 1 min-year) max-year))
             [max-year])
            marks
            (map #(hash-map :value % :label (str %)) mark-years)]
        [:div
         [lists/sub-header {:subheader "Years of Interest" :style {:z-index 0}}] 
         [:div {:style {:padding-left 16 :padding-right 16}}
          [i/slider
           {:id id
            :args
            {:value-label-display :auto
             :marks marks
             :sx {:max-width 500}}
            :step 1
            :min min-year
            :max max-year}]]]))))

(defn filter-panel []
  [input-panel
   {:id :filter-panel
    :start-closed true
    :header "Filters"
    :collapsable? true 
    :content 
    [grid/grid
     {:grid-args {:justify-content :space-evenly}
      :item-args {:elevation 0}
      :content
      [{:xs 5 :content [year-filter]}
       {:xs 5 :content [stric-boundary-filter]}
       {:xs 5 :content [area-filter :area-checkbox]}
       {:xs 5 :content [region-filter]}]}]}])

(defn filter-panel-conferences []
  [input-panel
   {:id :filter-panel-conferences
    :start-closed true
    :header "Filters"
    :collapsable? true
    :content
    [grid/grid
     {:grid-args {:justify-content :start}
      :item-args {:elevation 0}
      :content
      [{:xs 5 :content [area-filter :area-checkbox-conferences]}]}]}])

(reg-sub
 ::selected-countries
 :<- [::db/user-input-field [:region-checkbox]]
 :<- [::data/region-mapping]
 (fn [[selection-region-checkbox region-mapping]]
   (when (and selection-region-checkbox region-mapping) 
     (clojure.set/intersection
      selection-region-checkbox
      (into #{} (map #(keyword (:country-id %)) region-mapping))))))

(reg-sub
 ::selected-regions
 :<- [::db/user-input-field [:region-checkbox]]
 :<- [::data/region-mapping]
 (fn [[selection-region-checkbox region-mapping]]
   (when (and selection-region-checkbox region-mapping) 
     (clojure.set/intersection
      selection-region-checkbox
      (into #{} (map #(keyword (:region-id %)) region-mapping))))))

(reg-sub
 ::selected-areas
 :<- [::db/user-input-field [:area-checkbox]] 
 (fn [selection-area-checkbox]
   (when selection-area-checkbox
     (set (filter #(not (namespace %)) selection-area-checkbox)))))

(reg-sub
 ::selected-areas-conferences
 :<- [::db/user-input-field :area-checkbox-conferences]
 (fn [selection-area-checkbox]
   (when selection-area-checkbox
     (set (filter #(not (namespace %)) selection-area-checkbox)))))

(reg-sub
 ::selected-sub-areas
 :<- [::db/user-input-field [:area-checkbox]]
 (fn [selection-area-checkbox]
   (when selection-area-checkbox
     (set (mapv #(keyword (name %)) (filter namespace selection-area-checkbox))))))

(reg-sub
 ::selected-sub-areas-conferences
 :<- [::db/user-input-field :area-checkbox-conferences]
 (fn [selection-area-checkbox]
   (when selection-area-checkbox
     (set (mapv #(keyword (name %)) (filter namespace selection-area-checkbox))))))

(reg-sub
 ::selected-year-span
 :<- [::db/user-input-field [:selected-year]]
 (fn [selected-year-span]
   (when selected-year-span
     selected-year-span)))

(comment
  @(subscribe [::area-checkbox-content])
  @(subscribe [::selected-regions])
  @(subscribe [::selected-countries])
  @(subscribe [::selected-areas])
  @(subscribe [::selected-sub-areas]) 
  @(subscribe [::selected-year-span])
  @(subscribe [::db/user-input-field [:conferences]])
  @(subscribe [::selected-areas-conferences])
  @(subscribe [::selected-sub-areas-conferences]) 
  )