(ns app.cscollab.transformer
  (:require
   [app.cscollab.filter-panel :as filter-panel]
   [app.cscollab.data :as data]
   [app.db :as db]
   [app.util :as util]
   [re-frame.core :refer
    (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]))

(comment 
  (def selected-regions @(subscribe [::filter-panel/selected-regions]))
  (def selected-countries @(subscribe [::filter-panel/selected-countries]))
  (def selected-areas @(subscribe [::filter-panel/selected-areas]))
  (def selected-sub-areas @(subscribe [::filter-panel/selected-sub-areas]))
  (def strict-boundary @(subscribe [::db/user-input-field [:strict-boundary]]))
  (def collab @(subscribe [::data/collab]))
  (first collab)
  (count
   (transduce
    (apply comp
     [(filter #(and (contains? selected-countries (keyword (:a_country %)))
                   (contains? selected-countries (keyword (:b_country %)))))
      (filter #(contains? selected-sub-areas (keyword (:rec_sub_area %))))
      (filter #(> 2010 (:year %)))])
    conj [] collab))
  )