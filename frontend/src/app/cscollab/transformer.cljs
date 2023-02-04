(ns app.cscollab.transformer
  (:require
   [app.cscollab.filter-panel :as filter-panel]
   [app.cscollab.data :as data]
   [app.db :as db]
   [app.util :as util]
   [re-frame.core :refer
    (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]))

(reg-sub
 ::filtered-collab-9
 :<- [::filter-panel/selected-areas]
 :<- [::filter-panel/selected-sub-areas]
 :<- [::filter-panel/selected-regions]
 :<- [::filter-panel/selected-countries]
 :<- [::db/user-input-field [:strict-boundary]]
 :<- [::data/collab]
 :<- [::data/region-mapping]
 :<- [::data/area-mapping]
 (fn [[selected-areas selected-sub-areas selected-regions selected-countries
       strict-boundary? collab region-mapping area-mapping]]
   (when (and selected-areas selected-sub-areas selected-regions selected-countries
              collab region-mapping area-mapping)
     (let [cond-fn
           (fn [strict-boundary? & x]
             (reduce
              (if strict-boundary? #(and %1 %2) #(or %1 %2))
              x))
           sub-areas
           (clojure.set/union
            selected-sub-areas
            (set
             (map
              #(keyword (:sub-area-id %))
              (filter #(contains? selected-areas (keyword (:area-id %)))
                      area-mapping))))
           countries
           (clojure.set/union
            selected-countries
            (set
             (map
              #(keyword (:country-id %))
              (filter #(contains? selected-regions (keyword (:region-id %)))
                      region-mapping))))
           country-filter
           (filter #(cond-fn strict-boundary?
                             (contains? countries (keyword (:a_country %)))
                             (contains? countries (keyword (:b_country %)))))
           area-filter
           (filter #(contains? sub-areas (keyword (:rec_sub_area %))))
           year-filter
           (filter #(> (:year %) 2000))
           xform
           (apply comp
                  [country-filter
                   area-filter
                   year-filter])]
       (transduce
        xform
        conj [] collab)))))

(comment
  (count @(subscribe [::filtered-collab-9]))
  (def selected-regions @(subscribe [::filter-panel/selected-regions]))
  (def selected-countries @(subscribe [::filter-panel/selected-countries]))
  (def selected-areas @(subscribe [::filter-panel/selected-areas]))
  (def selected-sub-areas @(subscribe [::filter-panel/selected-sub-areas]))
  (def strict-boundary @(subscribe [::db/user-input-field [:strict-boundary]]))
  (def collab @(subscribe [::data/collab]))
  (def region-mapping @(subscribe [::data/region-mapping]))
  (first region-mapping)
  (def area-mapping @(subscribe [::data/area-mapping]))
  (first collab) 
  (def sub-areas
    (clojure.set/union
     selected-sub-areas
     (set
      (map
       #(keyword (:sub-area-id %))
       (filter #(contains? selected-areas (keyword (:area-id %)))
               area-mapping)))))
  (def countries 
    (clojure.set/union
     selected-countries
     (set
      (map
       #(keyword (:country-id %))
       (filter #(contains? selected-regions (keyword (:region-id %)))
               region-mapping)))))
  (count
   (transduce
    (apply comp
           [(filter #(and (contains? selected-countries (keyword (:a_country %)))
                          (contains? selected-countries (keyword (:b_country %)))))
            (filter #(contains? selected-sub-areas (keyword (:rec_sub_area %))))
            (filter #(> 2010 (:year %)))])
    conj [] collab))
  (def y (fn [strict-boundary? & x]
           (reduce
            (if strict-boundary? #(and %1 %2) #(or %1 %2))
            x)))
  (y false false true)
  (reduce #(or %1 %2) [true false])

  )