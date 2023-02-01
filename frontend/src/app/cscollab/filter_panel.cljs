(ns app.cscollab.filter-panel
  (:require
   [app.common.user-input :refer (input-panel)]
   [app.cscollab.data :as data]
   [app.db :as db]
   [app.util :as util]
   [re-frame.core :refer
    (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
   [app.components.lists :as lists]))


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
       {:id :all :label "All Areas" :style {:font-size 20 :font-weight :medium}}
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
         {:id :area-checkbox-1
          :list-args {:dense false :sx {:max-width 400 :width "100%"}}
          :content-sub content}]))))

(defn area-filter []
  [area-checkbox-list])

(defn filter-panel []
  [input-panel
   {:id :input-panel
    :start-closed false
    :header "Filters"
    :collapsable? true
    :content-args {:style
                   {:grid-template-columns "repeat(2, minmax(250px, 1fr))"}}
    :components
    [[area-filter]]}])

(comment @(subscribe [::area-checkbox-content]))