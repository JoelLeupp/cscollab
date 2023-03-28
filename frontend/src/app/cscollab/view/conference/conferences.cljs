(ns app.cscollab.view.conference.conferences
  (:require 
   [app.cscollab.data :as data]
   [app.components.lists :refer [collapse]]
   [app.db :as db]
   [app.components.button :as button]
   [reagent-mui.material.paper :refer [paper]]
   [clojure.walk :refer [postwalk]]
   [app.util :as util]
   [app.cscollab.panels.filter-panel :refer (filter-panel-conferences)]
   [reagent-mui.material.paper :refer [paper]]
   [app.components.lists :refer (nested-list)]
   [app.components.stack :refer (horizontal-stack)]
   ["@mui/material/ListItemText" :default mui-list-item-text]
   [re-frame.core :refer
    (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
   [reagent.core :as r]))


;; define conference page

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
                      :costum-label
                      [horizontal-stack
                       {:stack-args {:spacing 2}
                        :items [[:a {:href (dblp-conf-link (:id conf))}
                                 [:img {:src "img/dblp.png" :target "_blank"
                                        :width 10 :height 10 :padding 10}]]
                                [:> mui-list-item-text {:primary (:label conf)}]]}]
                      #_#_:label (r/as-element
                                  [:a {:href (dblp-conf-link (:id conf))
                                       :style {:text-decoration "none"}}
                                   (:label conf)])})})))})))]
       (concat [{:id "conference-list" :label  "List of Included Conferences"}] list-items)))))

(defn conferences-view []
  (let [list-content (subscribe [::list-content])]
    (fn []
      [:<>
       #_[filter-panel-conferences]
       [:div {:id "test"} [:a {:name "test"}]]
       [paper {:elevation 1 :sx {:padding 2 :background-color :white}}
        [:div 
         [nested-list
          {:id :conference-list
           :list-args {:dense false :sx {#_#_:background-color :white :max-width nil :width "100%"}}
           :content @list-content}]]]])))

(comment 
  (def area-mapping @(subscribe [::db/data-field :get-area-mapping]))
  ;; document.getElementById ('asru') .scrollIntoView (true)
  (first area-mapping))