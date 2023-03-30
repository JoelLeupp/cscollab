(ns app.cscollab.view.publications.publications
  (:require
   [app.cscollab.data :as data]
   [app.router :as router]
   [app.components.lists :as lists :refer [collapse]]
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
   ["@mui/material/IconButton" :default mui-icon-button]
   ["@mui/icons-material/ExpandLess" :default ic-expand-less]
   ["@mui/icons-material/ExpandMore" :default ic-expand-more]
   [app.components.feedback :as feedback]
   [re-frame.core :refer
    (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
   [reagent.core :as r]
   [clojure.string :as str]))

(defn update-data []
  (let [config @(subscribe [::common/filter-config])]
    (dispatch [::api/get-filtered-collab config])))

(defn expand-icon [path]
  (let [open? (subscribe [::db/ui-states-field (conj path :open?)])]
    (fn []
      [:> mui-icon-button
       {:on-click
        (fn []
          (dispatch [::db/update-ui-states (conj path :open?)
                     #(if % false true)]))}
       (if @open?
         [:> ic-expand-less]
         [:> ic-expand-more])])))

(defn dblp-proceeding-link
  "dblp link to the proceeding html page"
  [p]
  (let [[_ conf_id p_id] (clojure.string/split p #"/")]
    (str "https://dblp.org/db/conf/" conf_id "/" conf_id p_id ".html")))

(defn dblp-record-link
  "dblp link to the record html page"
  [rec] 
  (str "https://dblp.org/rec/" rec ".html"))



(defn publication-list []
  (let [rec-info (subscribe [::db/data-field :get-publication-info])
        area-mapping (subscribe [::db/data-field :get-area-mapping])]
    (fn []
      (let [id :publication-list
            area-mapping-idx (util/factor-out-key @area-mapping :conference-id)
            publications
            (map #(merge %
                         (select-keys
                          (get area-mapping-idx (keyword (:conf_id %)))
                          [:area-id :area-label :sub-area-id :sub-area-label]))
                 @rec-info)
            area-labels ["AI" "Systems" "Theory" "Interdisciplinary Areas"]]
        [:div {:style {:width "100%"}}
         (for [area area-labels]
           (let [sub-area-labels
                 (sort (into []
                             (set (map :sub-area-label
                                       (filter #(= (:area-label %) area) publications)))))]
             (when-not (empty? sub-area-labels)
               [:div 
                [:h2 {:style {:margin-top 10 :margin-bottom 10 #_#_:font-size 22}} area] 
                (for [sub-area-label sub-area-labels]
                  (let [rec-subarea (filter #(= (:sub-area-label %) sub-area-label) publications)]
                    [:div {:style {:width "100%" :margin-left 15}}
                     [:div {:style {:display :flex :justify-content :space-between :width "100%"}}
                      [:span {:style {:margin-top 10 :margin-bottom 10 :font-size 20}} sub-area-label]
                      [horizontal-stack 
                       {:box-args {:width "10%"}
                        :stack-args {:direction :row-reverse}
                        :items
                        [[expand-icon [id sub-area-label]]
                         [:span {:style {:display :flex :align-items :center}} (count rec-subarea)]]}]]
                     [collapse
                      {:sub (subscribe [::db/ui-states-field (conj [id sub-area-label] :open?)])
                       :div
                       (let [grouped-by-year (group-by :year (reverse (sort-by :year rec-subarea)))]
                         (for [[year records] grouped-by-year]
                           [:div {:style {:margin-left 15 :margin-right 15}}
                            [:div {:style {:display :flex :justify-content :space-between :width "100%"}}
                             [:span {:style {:margin-top 10 :margin-bottom 10 :font-size 18}} year]
                             [horizontal-stack
                              {:box-args {:width "10%"}
                               :stack-args {:direction :row-reverse}
                               :items
                               [[expand-icon [id sub-area-label year]]
                                [:span {:style {:display :flex :align-items :center}} (count records)]]}]]
                            [collapse
                             {:sub (subscribe [::db/ui-states-field (conj [id sub-area-label year] :open?)])
                              :div
                              (let [grouped-by-p (group-by (juxt :p_title :p_id)
                                                           (sort-by :p_title records))]
                                (for [[[p-title p-id] records-p] grouped-by-p]
                                  [:div {:style {:margin-left 15 :margin-right 15}}
                                   [:div {:style {:display :flex :justify-content :space-between :width "100%"}}
                                    [horizontal-stack
                                     {:stack-args {:spacing 2}
                                      :items [[:a {:href (dblp-proceeding-link p-id) :style {:display :flex :align-items :center}}
                                               [:img {:src "img/dblp.png" :target "_blank"
                                                      :width 10 :height 10 :padding 0}]]
                                              [:span {:style {:margin-top 10 :margin-bottom 10 :font-size 17}} p-title]]}]
                                    [horizontal-stack
                                     {:box-args {:width "10%"}
                                      :stack-args {:direction :row-reverse}
                                      :items
                                      [[expand-icon [id sub-area-label year p-id]]
                                       [:span {:style {:display :flex :align-items :center}} (count records-p)]]}]]
                                   [collapse
                                    {:sub (subscribe [::db/ui-states-field (conj [id sub-area-label year p-id] :open?)])
                                     :div
                                     [:div {:style {:margin-left 15 :margin-right 15}}
                                      (for [{:keys [rec_id rec_title]} (sort-by :rec_title records-p)]
                                        [horizontal-stack
                                         {:stack-args {:spacing 2}
                                          :items [[:a {:href (dblp-record-link rec_id) :style {:display :flex :align-items :center}}
                                                   [:img {:src "img/dblp.png" :target "_blank"
                                                          :width 10 :height 10 :padding 0}]]
                                                  [:span {:style {:margin-top 10 :margin-bottom 10 :font-size 16}} rec_title]]}])]}]]))}]]))}]]))])))]))))

(comment 
  @(subscribe [::db/ui-states-field [:publication-list]]))

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
  (first publications)
  (def grouped-p
    (filter #(and (= (:area-id %) "ai") (= (:year %) 2022) (= (:sub-area-id %) "ai")) publications))
  (map first (group-by (juxt :p_title :p_id)
                 (sort-by :p_title grouped-p)))
  (for [[[p-title p-id] records-p] (group-by (juxt :p_title :p_id)
                                            (sort-by :p_title grouped-p))]
    p-title)
  (def area-ids ["ai" "systems" "theory" "interdiscip"])
  (for [area area-ids]
    (let [sub-area-labels
          (sort (into []
                      (set (map :sub-area-label
                                (filter #(= (:area-id %) area) publications)))))]
      (for [sub-area-label sub-area-labels]
        (count (filter #(= (:sub-area-label %) sub-area-label) publications)))))

  )