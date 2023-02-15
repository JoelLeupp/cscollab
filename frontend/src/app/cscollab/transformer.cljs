(ns app.cscollab.transformer
  (:require
   [app.cscollab.filter-panel :as filter-panel]
   [app.components.button :as button]
   [app.cscollab.data :as data]
   [app.db :as db]
   [app.util :as util]
   [reagent.core :as r]
   [re-frame.core :refer
    (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]))

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


(defn counter []
  (let [n (count @(subscribe [::filtered-collab]))]
    (fn []
      [:div {:style {:text-align :center}}
       (str "Number of collaborations based on filters: " n)])))

(defn collab-count []
  (let [update-view (r/atom 0)] 
    (fn []
      (js/console.log  @update-view)
      [:div
       [:div {:style {:position :relative :width 0 :height 0}}
        [button/update-button
         {:on-click #(swap! update-view inc)
          :style {:padding 20 :z-index 999}}]]
       ^{:key @update-view}
       [counter]])))


(comment
  (first (weighted-collab {:insti? false}))
  (def filtered-collab @(subscribe [::filtered-collab]))
  (first filtered-collab)
  ()
  (def author-inst-map 
    (zipmap (map :pid csauthors) (map :institution csauthors)))
  (get author-inst-map "92/6595")
  (first filtered-collab)
  (def insti? true)
  
  (map
   (fn [[[a_pid b_pid] values]]
     {:node/m a_pid
      :node/n b_pid
      :weight (count values)})
   (group-by (if insti?
               (juxt :a_inst :b_inst)
               (juxt :a_pid :b_pid))
             filtered-collab))
  (first )
  (count @(subscribe [::filtered-collab-10]))
  @(subscribe [::data/collab-year-span])
  (def selected-regions @(subscribe [::filter-panel/selected-regions]))
  (def selected-countries @(subscribe [::filter-panel/selected-countries]))
  (def selected-areas @(subscribe [::filter-panel/selected-areas]))
  (def selected-sub-areas @(subscribe [::filter-panel/selected-sub-areas]))
  (def strict-boundary @(subscribe [::db/user-input-field [:strict-boundary]]))
  (def collab @(subscribe [::data/collab]))
  (def csauthors @(subscribe [::data/csauthors]))
  (def author-inst-map 
    (zipmap (map :pid csauthors) (map :institution csauthors)))
  (get author-inst-map "92/6595")
  (def region-mapping @(subscribe [::data/region-mapping]))
  (first region-mapping)
  )