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

;; define the publication explorer

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

(defn dblp-conf-link
  "dblp link to the conference html page"
  [conf]
  (str "https://dblp.org/db/conf/" (name conf) "/index.html"))

(defn get-authors [rec]
  (let [csauthors @(subscribe [::db/data-field :get-csauthors])
        pid->name (zipmap (map :pid csauthors) (map :name csauthors))
        filtered-collab @(subscribe [::db/data-field :get-filtered-collab])
        records (filter (fn [x] (= (:rec_id x) rec)) filtered-collab)]
    (sort (map #(get pid->name %)
               (vec (clojure.set/union (set (map :a_pid records))
                                       (set (map :b_pid records))))))))

(defn year-records [year-open? id sub-area-label year records]
  (fn []
    (when @year-open?
      (let [grouped-by-p (group-by (juxt :conf_title :conf_id) #_(juxt :p_title :p_id)
                                   (sort-by :conf_title #_:p_title records))]
        [:div
         (for [[[p-title p-id] records-p] grouped-by-p]
           [:div {:style {:margin-left 15 :margin-right 15}}
            [:div {:style {:display :flex :justify-content :space-between :width "100%"}}
             [horizontal-stack
              {:stack-args {:spacing 2}
               :items [[:a {:href (dblp-conf-link p-id) #_(dblp-proceeding-link p-id) :style {:display :flex :align-items :center}}
                        [:img {:src "img/dblp.png" :target "_blank"
                               :width 10 :height 10 :padding 0}]]
                       [:span {:style {:margin-top 10 :margin-bottom 10 :font-size 17}} p-title]]}]
             [horizontal-stack
              {:box-args {:width "10%"}
               :stack-args {:direction :row-reverse}
               :items
               [[expand-icon [id sub-area-label year p-id]]
                [:span {:style {:display :flex :align-items :center}} (count records-p)]]}]]
            (let [open? (subscribe [::db/ui-states-field (conj [id sub-area-label year p-id] :open?)])]
              [collapse
               {:sub open?
                :div
                ^{:key @open?}
                [:div {:style {:margin-left 15 :margin-right 15}}
                 (for [{:keys [rec_id rec_title]} (sort-by :rec_title records-p)]
                   [horizontal-stack
                    {:stack-args {:spacing 2}
                     :items [[:a {:href (dblp-record-link rec_id) :style {:display :flex :align-items :center}}
                              [:img {:src "img/dblp.png" :target "_blank"
                                     :width 10 :height 10 :padding 0}]]
                             [:span {:style {:margin-top 10 :margin-bottom 10 :font-size 16}} rec_title
                              (when @open?
                                [:span {:style {:margin-left 5 :vertical-align :bottom :font-size 14}}
                                 [:i
                                  (clojure.string/join ", " (get-authors rec_id))]])]]}])]}])])]))))

(defn publication-list []
  (let [rec-info (subscribe [::db/data-field :get-publication-info])
        area-mapping (subscribe [::db/data-field :get-area-mapping])
        ]
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
                            (let [year-open? (subscribe [::db/ui-states-field (conj [id sub-area-label year] :open?)])]
                              [collapse
                               {:sub year-open?
                                :div
                                [year-records year-open? id sub-area-label year records]}])]))}]]))])))]))))


(defn select-author []
  (let [filtered-collab (subscribe [::db/data-field :get-filtered-collab])
        csauthors (subscribe [::db/data-field :get-csauthors])]
    (fn []
      (let [pid->name (zipmap (map :pid @csauthors) (map :name @csauthors))
            authors (sort-by
                     :label
                     (mapv #(identity {:value % :label (get pid->name %)})
                          (vec (clojure.set/union (set (map :a_pid @filtered-collab))
                                                  (set (map :b_pid @filtered-collab))))))]
        [i/autocomplete
         {:id :publication-author
          :keywordize-values false
          :label "select an author"
          :style {:width "50vw" :max-width 350}
          :options authors}]))))

(defn select-collab-author-a []
  (let [filtered-collab (subscribe [::db/data-field :get-filtered-collab])
        csauthors (subscribe [::db/data-field :get-csauthors])]
    (fn []
      (let [pid->name (zipmap (map :pid @csauthors) (map :name @csauthors))
            authors (sort-by
                     :label
                     (mapv #(identity {:value % :label (get pid->name %)})
                           (vec (clojure.set/union (set (map :a_pid @filtered-collab))
                                                   (set (map :b_pid @filtered-collab))))))]
        [i/autocomplete
         {:id :select-collab-author-a
          :keywordize-values false
          :label "select first author"
          :style {:width "50vw" :max-width 350}
          :options authors}]))))

(defn select-collab-author-b []
  (let [filtered-collab (subscribe [::db/data-field :get-filtered-collab])
        csauthors (subscribe [::db/data-field :get-csauthors])
        select-collab-author (subscribe [::db/user-input-field :select-collab-author-a])]
    (fn []
      (let [pid->name (zipmap (map :pid @csauthors) (map :name @csauthors))
            authors (sort-by
                     :label
                     (mapv #(identity {:value % :label (get pid->name %)})
                           (vec (clojure.set/union
                                 (set (map :a_pid 
                                           (filter (fn [x] (= (:b_pid x) @select-collab-author)) @filtered-collab)))
                                 (set (map :b_pid 
                                           (filter (fn [x] (= (:a_pid x) @select-collab-author)) @filtered-collab)))))))]
        [i/autocomplete
         {:id :select-collab-author-b
          :keywordize-values false
          :label "select second author"
          :style {:width "50vw" :max-width 350}
          :options authors}]))))

(defn select-inst []
  (let [filtered-collab (subscribe [::db/data-field :get-filtered-collab])]
    (fn []
      (let [institution (sort-by
                     :label
                     (mapv #(identity {:value % :label %})
                           (vec (clojure.set/union (set (map :a_inst @filtered-collab))
                                                   (set (map :b_inst @filtered-collab))))))]
        [i/autocomplete
         {:id :publication-inst
          :keywordize-values false
          :label "select an Institution"
          :style {:width "50vw" :max-width 350}
          :options institution}]))))

(defn select-collab-inst-a []
  (let [filtered-collab (subscribe [::db/data-field :get-filtered-collab])]
    (fn []
      (let [institution (sort-by
                         :label
                         (mapv #(identity {:value % :label %})
                               (vec (clojure.set/union (set (map :a_inst @filtered-collab))
                                                       (set (map :b_inst @filtered-collab))))))]
        [i/autocomplete
         {:id :select-collab-inst-a
          :keywordize-values false
          :label "select first Institution"
          :style {:width "50vw" :max-width 350}
          :options institution}]))))

(defn select-collab-inst-b []
  (let [filtered-collab (subscribe [::db/data-field :get-filtered-collab])
        select-collab-inst (subscribe [::db/user-input-field :select-collab-inst-a])]
    (fn []
      (let [institution
            (sort-by
             :label
             (mapv #(identity {:value % :label %})
                   (vec (clojure.set/union
                         (set (map :a_inst
                                   (filter (fn [x] (= (:b_inst x) @select-collab-inst)) @filtered-collab)))
                         (set (map :b_inst
                                   (filter (fn [x] (= (:a_inst x) @select-collab-inst)) @filtered-collab)))))))]
        [i/autocomplete
         {:id :select-collab-inst-b
          :keywordize-values false
          :label "select second Institution"
          :style {:width "50vw" :max-width 350}
          :options institution}]))))

(defn get-ids []
  (let [filtered-collab @(subscribe [::db/data-field :get-filtered-collab])
        select-publication @(subscribe [::db/user-input-field :select-publication])
        publication-author @(subscribe [::db/user-input-field :publication-author])
        publication-inst @(subscribe [::db/user-input-field :publication-inst])
        select-collab-author-a @(subscribe [::db/user-input-field :select-collab-author-a])
        select-collab-author-b @(subscribe [::db/user-input-field :select-collab-author-b])
        select-collab-inst-a @(subscribe [::db/user-input-field :select-collab-inst-a])
        select-collab-inst-b @(subscribe [::db/user-input-field :select-collab-inst-b])]
    (vec
     (set
      (case select-publication
        :all (map :rec_id filtered-collab)
        :authors (map :rec_id (filter #(or (= publication-author (:a_pid %))
                                           (= publication-author (:b_pid %))) filtered-collab))
        :inst (map :rec_id (filter #(or (= publication-inst (:a_inst %))
                                        (= publication-inst (:b_inst %))) filtered-collab))
        :author-collab (when (and select-collab-author-a select-collab-author-b)
                         (map :rec_id
                              (filter #(or (and (= select-collab-author-a (:a_pid %))
                                                (= select-collab-author-b (:b_pid %)))
                                           (and (= select-collab-author-a (:b_pid %))
                                                (= select-collab-author-b (:a_pid %)))) filtered-collab)))
        :inst-collab (when (and select-collab-inst-a select-collab-inst-b)
                       (map :rec_id
                            (filter #(or (and (= select-collab-inst-a (:a_inst %))
                                              (= select-collab-inst-b (:b_inst %)))
                                         (and (= select-collab-inst-a (:b_inst %))
                                              (= select-collab-inst-b (:a_inst %)))) filtered-collab)))
        (map :rec_id filtered-collab))))))

(defn publication-view []
  (let [loading-collab? (subscribe [::db/loading? :get-filtered-collab])
        loading-info? (subscribe [::db/loading? :get-publication-info])
        rec-info (subscribe [::db/data-field :get-publication-info])
        select-publication (subscribe [::db/user-input-field :select-publication])
        select-collab-author (subscribe [::db/user-input-field :select-collab-author-a])
        select-collab-inst (subscribe [::db/user-input-field :select-collab-inst-a])
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
                   (dispatch [::api/get-publication-info (get-ids)]))))
    (add-watch select-collab-author ::collab-author
               (fn [_ _ _ _]
                 (dispatch [::db/set-user-input :select-collab-author-b nil])))
    (add-watch select-collab-inst ::collab-inst
               (fn [_ _ _ _]
                 (dispatch [::db/set-user-input :select-collab-inst-b nil])))
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
          [:div {:style {:display :flex :justify-content :flex-start :background-color :white :gap 20}}
           [i/autocomplete
            {:id :select-publication
             :label "Publication Filters"
             :style {:width 350}
             :options
             [{:value :all :label "All"}
              {:value :authors :label "Author"}
              {:value :inst :label "Institution"}
              {:value :author-collab :label "Collaboration between Authors"}
              {:value :inst-collab :label "Collaboration between Institutions"}]}]
           (when (= :authors @select-publication)
             {:key @select-publication}
             [select-author])
           (when (= :inst @select-publication)
             {:key @select-publication}
             [select-inst])
           (when (= :author-collab @select-publication)
             {:key @select-publication}
             [select-collab-author-a])
           (when (and (= :author-collab @select-publication) @select-collab-author)
             {:key @select-publication}
             [select-collab-author-b])
           (when (= :inst-collab @select-publication)
             {:key @select-publication}
             [select-collab-inst-a])
           (when (and (= :inst-collab @select-publication) @select-collab-inst)
             {:key @select-publication}
             [select-collab-inst-b])]
          [button/update-button
           {:on-click #(do
                         (swap! reset inc)
                         (dispatch [::db/set-ui-states :publication-list nil])
                         (update-data))}]]
         [:div {:style {:display :flex :justify-content :left :margin-top 20}}
          ^{:key [(count @rec-info)]}
          [publication-list]]]]])))

