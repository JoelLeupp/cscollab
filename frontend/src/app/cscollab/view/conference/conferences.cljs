(ns app.cscollab.view.conference.conferences
  (:require
   [app.cscollab.data :as data]
   [app.components.lists :refer [collapse]]
   [app.db :as db]
   [app.components.button :as button]
   [reagent-mui.material.paper :refer [paper]]
   [app.components.colors :refer [colors area-color sub-area-color]]
   [app.components.inputs :as i]
   [clojure.walk :refer [postwalk]]
   [app.util :as util]
   [app.cscollab.panels.filter-panel :refer (filter-panel-conferences)]
   [reagent-mui.material.paper :refer [paper]]
   [app.components.lists :as lists :refer (nested-list)]
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
 :<- [::db/user-input-field :show-conference]
 :<- [::db/data-field :get-area-mapping]
 (fn
   [[nested-area selected-conf area-mapping]]
   "generates the checkbox content structure"
   (when (and nested-area area-mapping)
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
           :costum-label
           [:div {:style {:width "90%" :display :flex :justify-content :space-between}}
            [:>  mui-list-item-text {:primary (:label area)}]
            [:>  mui-list-item-text {:primary (count (filter #(= (:area-id %) (name (:id area))) area-mapping)) 
                                     :primary-typography-props {:text-align :right}}]]}
          (when (:sub-areas area)
            {:children
             (for [sub-area (:sub-areas area)]
               (merge
                {:id (util/s->id (:id sub-area))
                 :costum-label
                 [:div {:style {:width "90%" :display :flex :justify-content :space-between}}
                  [:>  mui-list-item-text {:primary (:label sub-area)}]
                  [:>  mui-list-item-text {:primary (count (filter #(= (:sub-area-id %) (name (:id sub-area))) area-mapping))
                                           :primary-typography-props {:text-align :right}}]]}
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
                                [:> mui-list-item-text {:primary (:label conf)
                                                        :style (when (= (:id conf) selected-conf)
                                                                 {:color (:second colors)})}]]}]})})))})))]
       (concat [{:id "conference-list"
                 :costum-label
                 [:div {:style {:width "90%" :display :flex :justify-content :space-between}}
                  [:>  mui-list-item-text {:primary "List of Conferences by Area"
                                           :primary-typography-props {:font-size 17}}]
                  [:>  mui-list-item-text {:primary "Count"
                                           :primary-typography-props {:text-align :right :font-size 17}}]]}] list-items)))))

(defn show-conference []
  (let [area-mapping (subscribe [::db/data-field :get-area-mapping])
        selected-conf (subscribe [::db/user-input-field :show-conference])]
    (fn []
      (when @area-mapping
        (let [options
              (sort-by
               :label
               (vec (set
                     (map #(identity {:value (keyword (:conference-id %)) :label (:conference-title %)})
                          @area-mapping))))
              conf-map (util/factor-out-key @area-mapping :conference-id)
              {:keys [area-id sub-area-id]} (get conf-map @selected-conf)]
          [:div 
           [horizontal-stack
            {:items
             (list
              [i/autocomplete
               {:id :show-conference
                :keywordize-values true
                :label "search for a conference"
                :style {:width "50vw" :max-width 900}
                :options options}]
              [button/button
               {:text "show"
                :on-click  #(when @selected-conf 
                              (dispatch [::db/set-ui-states :conference-list
                                         {(keyword area-id)
                                          {:open? true
                                           (keyword sub-area-id)
                                           {:open? true}}}]))}])}]])))))

  (defn conferences-view []
    (let [list-content (subscribe [::list-content])]
      (fn []
        [:<> 
         [paper {:elevation 0 :sx {:padding 2 :background-color :white}}
          [:div {:style {:display :flex :justify-content :center}}]
          [:div 
           [show-conference]
           [nested-list
            {:id :conference-list
             :list-args {:dense false :sx {:max-width 1000 :width "100%"}}
             :content @list-content}]]]])))

