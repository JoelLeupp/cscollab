(ns app.cscollab.filter-panel
  (:require
   [app.common.user-input :refer (input-panel)]
   [app.cscollab.data :as data]
   [app.db :as db]
   [app.util :as util]
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



(defn area-checkbox-list []
  (let
   [content (subscribe [::area-checkbox-content])]
    (fn []
      (when @content 
        [lists/checkbox-list
         {:id :area-checkbox
          :subheader "Select Computer Science Areas"
          :style {:subheader {:font-size 18}}
          :list-args {:dense false :sx {:max-width 400 :width "100%"}}
          :content @content}]))))

(defn area-filter []
  [area-checkbox-list])


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
          :list-args {:dense false :sx {:max-width 400 :width "100%"}}
          :namespace-id? false
          :content @content}]))))

(defn region-filter []
  [:div
   [region-checkbox-list]
   [:div {:title "Only consider collaborations within the selected regions/countries"}
    [i/switch
     {:id :strict-boundary
      #_:label-off
      :label-on "strict country restriction"}]]])

(defn filter-panel []
  [input-panel
   {:id :input-panel
    :start-closed false
    :header "Filters"
    :collapsable? true
    :content-args {:style
                   {:grid-template-columns "repeat(2, minmax(250px, 1fr))"}}
    :components
    [[area-filter]
     [region-filter]]}])

(reg-sub
 ::selected-countries
 :<- [::db/user-input-field [:region-checkbox]]
 :<- [::data/region-mapping]
 (fn [[selection-region-checkbox region-mapping]]
   (when (and selection-region-checkbox region-mapping)
     (vec
      (clojure.set/intersection
       selection-region-checkbox
       (into #{} (map #(keyword (:country-id %)) region-mapping)))))))

(reg-sub
 ::selected-regions
 :<- [::db/user-input-field [:region-checkbox]]
 :<- [::data/region-mapping]
 (fn [[selection-region-checkbox region-mapping]]
   (when (and selection-region-checkbox region-mapping)
     (vec
      (clojure.set/intersection
       selection-region-checkbox
       (into #{} (map #(keyword (:region-id %)) region-mapping)))))))

(reg-sub
 ::selected-areas
 :<- [::db/user-input-field [:area-checkbox]] 
 (fn [selection-area-checkbox]
   (when selection-area-checkbox
     (vec (filter #(not (namespace %)) selection-area-checkbox)))))

(reg-sub
 ::selected-sub-areas
 :<- [::db/user-input-field [:area-checkbox]]
 (fn [selection-area-checkbox]
   (when selection-area-checkbox
     (mapv #(keyword (name %)) (filter namespace selection-area-checkbox)))))


(comment
  @(subscribe [::area-checkbox-content])
  @(subscribe [::selected-regions])
  @(subscribe [::selected-countries])
  @(subscribe [::selected-areas])
  @(subscribe [::selected-sub-areas]) 
  )