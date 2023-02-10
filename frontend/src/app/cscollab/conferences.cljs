(ns app.cscollab.conferences
  (:require 
   [app.cscollab.data :as data]
   [app.components.lists :refer [collapse]]
   [app.db :as db]
   [app.components.button :as button]
   [reagent-mui.material.paper :refer [paper]]
   [clojure.walk :refer [postwalk]]
   [app.util :as util]
   [app.cscollab.filter-panel :refer (filter-panel-conferences)]
   [reagent-mui.material.paper :refer [paper]]
   [app.components.lists :refer (nested-list)]
   [re-frame.core :refer
    (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
   [reagent.core :as r]))


(defn dblp-conf-link
  "dblp link to the conference html page"
  [conf]
  (str "https://dblp.org/db/conf/" (name conf) "/index.html"))

(reg-sub
 ::list-content
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
       list-items
       (for [area sorted-nested-area]
         (merge
          {:id (util/s->id (:id area))
           :label (:label area)}
          (when (:sub-areas area)
            {:children
             (for [sub-area (:sub-areas area)]
               (merge
                {:id (util/s->id (:id sub-area))
                 :label (:label sub-area)}
                (when (:conferences sub-area)
                  {:children
                   (for [conf (:conferences sub-area)]
                     {:id (util/s->id (:id conf))
                      :label (r/as-element 
                              [:a {:href (dblp-conf-link (:id conf))
                                   :style {:text-decoration "none"}} 
                                            (:label conf)])})})))})))]
       list-items))))

(defn conferences-view []
  (let [list-content (subscribe [::list-content])]
    (fn []
      [:<>
       [filter-panel-conferences]
       [paper {:elevation 1 :sx {:padding 5 :background-color :white}}
        [nested-list
         {:id :conference-list
          :list-args {:dense false :sx {#_#_:background-color :white :max-width nil :width "100%"}}
          :content @list-content}]]])))

(comment
  (def list-content @(subscribe [::list-content]))
  (nested-list {:id :test
                :content list-content})
  @(subscribe [::db/ui-states])
  
  (second (postwalk (fn [x] x) (first list-content)))
  (first @(subscribe [::data/area-mapping]))
  (def nested-area @(subscribe [::data/nested-area]))
  (loop [l []
         cnt 10]
    (if (= cnt 0)
      l
      (recur (conj l (+ cnt (last l))) (dec cnt))))
  (loop [list-items []
         content (first list-content)]
    (if-not (:children content)
      list-items
      (do
        #_(print (:children content))
        (recur (conj list-items (:label content)) (first (:children content))))))

  (def nested-list-test [{:id 1 :c [{:id 2} {:id 3}]}
                         {:id 4 :c [{:id 8} {:id 9 :c [{:id 11}]}]}])

  (defn extract [nested-list-test indent]
    (for [c nested-list-test]
      (vec (concat
            [{:id (:id c) :indent indent}]
            (when (:c c) 
              (extract (:c c) (+ indent 4)))))))
  
  (extract nested-list-test 0)
  

  nested-area
  )