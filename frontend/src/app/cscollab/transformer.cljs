(ns app.cscollab.transformer
  (:require
   [app.cscollab.panels.filter-panel :as filter-panel]
   [app.components.button :as button]
   [app.cscollab.data :as data]
   [app.db :as db]
   [app.util :as util]
   [reagent.core :as r]
   [re-frame.core :refer
    (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]))

;; data transformations

(reg-sub
 ::filtered-collab
 :<- [::filter-panel/selected-areas]
 :<- [::filter-panel/selected-sub-areas]
 :<- [::filter-panel/selected-regions]
 :<- [::filter-panel/selected-countries]
 :<- [::db/user-input-field [:strict-boundary]]
 :<- [::data/collab]
 :<- [::data/region-mapping]
 :<- [::data/area-mapping]
 :<- [::filter-panel/selected-year-span]
 (fn [[selected-areas selected-sub-areas selected-regions selected-countries
       strict-boundary? collab region-mapping area-mapping year-span]]
   (when (and selected-areas selected-sub-areas selected-regions selected-countries
              collab region-mapping area-mapping year-span)
     (let [[min-year max-year] year-span
           cond-fn
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
           (filter #(and (>= (:year %) min-year) (<= (:year %) max-year)))
           xform
           (apply comp
                  [country-filter
                   area-filter
                   year-filter])]
       (transduce
        xform
        conj [] collab)))))

(defn weighted-collab [{:keys [insti?]}] 
  (let [filtered-collab @(subscribe [::filtered-collab])]
    (map
     (fn [[[m n] values]]
       {:node/m m
        :node/n n
        :weight (count (set (map :rec_id values)))})
     (group-by (if insti?
                 (juxt :a_inst :b_inst)
                 (juxt :a_pid :b_pid))
               filtered-collab))))


