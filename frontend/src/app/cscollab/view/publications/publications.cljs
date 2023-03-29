(ns app.cscollab.view.publications.publications
  (:require
   [app.cscollab.data :as data]
   [app.router :as router]
   [app.components.lists :refer [collapse]]
   [app.db :as db]
   [app.components.inputs :as i]
   [app.components.button :as button]
   [reagent-mui.material.paper :refer [paper]]
   [clojure.walk :refer [postwalk]]
   [app.cscollab.common :as common]
   [app.cscollab.api :as api]
   [app.util :as util]
   ["@mui/material/MenuItem" :default mui-menu-item]
   ["@mui/material/ListItem" :default mui-list-item]
   ["@mui/material/ListSubheader" :default mui-list-subheader]
   ["@mui/material/ListItemText" :default mui-list-item-text]
   [app.cscollab.panels.filter-panel :as filter-panel]
   [app.components.stack :refer (horizontal-stack)]
   [app.cscollab.panels.filter-panel :refer (filter-panel-conferences)]
   [reagent-mui.material.paper :refer [paper]]
   [app.components.lists :refer (nested-list)]
   [app.components.feedback :as feedback]
   [re-frame.core :refer
    (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
   [reagent.core :as r]))

(defn update-data []
  (let [config @(subscribe [::common/filter-config])]
    (dispatch [::api/get-filtered-collab config])))

(defn publication-list []
  (let [rec-info (subscribe [::db/data-field :get-publication-info])
        area-mapping (subscribe [::db/data-field :get-area-mapping])]
    (fn []
      (let [area-mapping-idx (util/factor-out-key @area-mapping :conference-id)
            publications
            (map #(merge %
                         (select-keys
                          (get area-mapping-idx (keyword (:conf_id %)))
                          [:area-id :area-label :sub-area-id :sub-area-label]))
                 @rec-info)
            area-labels ["AI" "Systems" "Theory" "Interdisciplinary Areas"]]
        [:div
         (for [area area-labels]
           (let [sub-area-labels
                 (sort (into []
                             (set (map :sub-area-label
                                       (filter #(= (:area-label %) area) publications)))))]
             (when-not (empty? sub-area-labels)
               [:div [:h2 area]
                (for [sub-area-label sub-area-labels]
                  (let [rec-subarea (filter #(= (:sub-area-label %) sub-area-label) publications)]
                    [:div
                     [:h3 sub-area-label]
                     (let [grouped-by-year (group-by :year (reverse (sort-by :year rec-subarea)))]
                       (for [[year records] grouped-by-year]
                         [:div 
                          [:h4 year]
                          (let [grouped-by-conf (group-by (juxt :conf_id :conf_title) 
                                                          (sort-by :conf_title records))]
                            (for [[[conf-id conf-tite] records-conf] grouped-by-conf]
                              [:div
                               [:h4 conf-tite]
                               (let [grouped-by-p (group-by (juxt :p_id :p_title)
                                                               (sort-by :p_title records-conf))]
                                 (for [[[p-id p-tite] records-p] grouped-by-p]
                                   [:div
                                    [:h4 p-tite] 
                                    (for [{:keys [rec_id rec_title]} (sort-by :rec_title records-p)]
                                      [:div [:span rec_title]
                                       [:br]])]))]))]))]))])))]))))

(defn publication-view []
  (let [loading-collab? (subscribe [::db/loading? :get-filtered-collab])
        loading-info? (subscribe [::db/loading? :get-publication-info])
        rec-info (subscribe [::db/data-field :get-publication-info])
        reset (atom 0)]
    (add-watch loading-info? ::data-loading
               (fn [_ _ _ data-loading?]
                 (if data-loading?
                   (dispatch [::feedback/open :data-loading])
                   (dispatch [::feedback/close :data-loading]))))
    (add-watch loading-collab? ::collab-loading
               (fn [_ _ _ data-loading?]
                 (if data-loading?
                   (dispatch [::feedback/open :data-loading]) 
                   (dispatch [::api/get-publication-info
                              (map :rec_id @(subscribe [::db/data-field :get-filtered-collab]))]))))
    (update-data)
    (fn []
      [:div
       [feedback/feedback {:id :data-loading
                           :anchor-origin {:vertical :top :horizontal :center}
                           :status :info
                           :auto-hide-duration nil
                           :message "Data is loading, please wait."}]
       [filter-panel/filter-panel-author]
       [paper {:elevation 1 :sx {:padding 4 :background-color :white :min-height "60vh"}}
        ^{:key [@loading-collab? @reset @loading-info?]}
        [:div
         [:div {:style {:display :flex :justify-content :space-between :background-color :white}}
          [:div {:style {:display :flex :justify-content :flex-start :background-color :white :gap 50}}]
          [button/update-button
           {:on-click #(do (swap! reset inc) (update-data))}]]
         [:div {:style {:display :flex :justify-content :left :margin-top 20}}
          ^{:key [@rec-info]}
          [publication-list]]]]])))

(comment
  (def rec-info @(subscribe [::db/data-field :get-publication-info]))
  (def area-mapping @(subscribe [::db/data-field :get-area-mapping]))
  (set (map :area-label area-mapping))
  (def area-mapping-idx (util/factor-out-key area-mapping :conference-id))
  (map #(merge %
               (select-keys
                (get area-mapping-idx (keyword (:conf_id %)))
                [:area-id :area-label :sub-area-id :sub-area-label]))
       rec-info)
  (def publications
    (map #(merge %
                 (select-keys
                  (get area-mapping-idx (keyword (:conf_id %)))
                  [:area-id :area-label :sub-area-id :sub-area-label]))
         rec-info))
  (def area-ids ["ai" "systems" "theory" "interdiscip"])
  (for [area area-ids]
    (let [sub-area-labels
          (sort (into []
                      (set (map :sub-area-label
                                (filter #(= (:area-id %) area) publications)))))]
      (for [sub-area-label sub-area-labels]
        (count (filter #(= (:sub-area-label %) sub-area-label) publications)))))

  )