(ns app.cscollab.view.authors.authors
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

;; define author page

(defn dblp-author-page [pid]
  (str "https://dblp.org/pid/" pid ".html"))

(defn country-selection []
  (let [region-mapping (subscribe [::db/data-field :get-region-mapping])
        selected-countries @(subscribe [::filter-panel/selected-countries])
        selected-regions @(subscribe [::filter-panel/selected-regions])]
    (fn []
      (when  @region-mapping
        (let [
              selected-region-mapping (filter #(or (contains? selected-countries (util/s->id (:country-id %)))
                                                   (contains? selected-regions (util/s->id (:region-id %)))) @region-mapping)
              nested-regions
              (mapv
               #(into
                 {}
                 [{:id
                   (util/s->id (-> % first first))
                   :label
                   (-> % first second)}
                  {:countries
                   (sort-by
                    :label
                    (mapv
                     (fn [[k _]]
                       (hash-map :id (util/s->id (first k)) :label (second k)))
                     (group-by (juxt :country-id :country-name) (second %))))}])
               (group-by (juxt :region-id :region-name) (filter #(not (or (= (:region-id %) "wd")
                                                                          (= (:region-id %) "dach"))) selected-region-mapping)))]
          [i/select {:id :country-selection
                     :label-id :country-selection
                     :label "select a country"
                     :keywordize-values true
                     :form-args {:style {:width 400}}
                     :select-args {:MenuProps {:PaperProps {:style {:max-height 400}}}}
                     :option
                     (for [item nested-regions]
                       (list [:> mui-list-subheader {:sx {:font-size 16}} (:label item)]
                             (for [country (:countries item)]
                               [:> mui-menu-item {:value (keyword (:id country))} (:label country)])))}])))))

(defn nested-authors [country-id]
  (let [filtered-collab @(subscribe [::db/data-field :get-filtered-collab])
        region-mapping @(subscribe [::db/data-field :get-region-mapping])
        csauthors @(subscribe [::db/data-field :get-csauthors])
        pid->name (zipmap (map :pid csauthors) (map :name csauthors))
        selected-countries @(subscribe [::filter-panel/selected-countries])
        selected-regions @(subscribe [::filter-panel/selected-regions])
        selected-region-mapping (filter #(or (contains? selected-countries (util/s->id (:country-id %)))
                                             (contains? selected-regions (util/s->id (:region-id %)))) region-mapping)
        author-inst (vec
                     (set
                      (concat
                       (map #(identity {:pid (:a_pid %)
                                        :country  (:a_country %)
                                        :inst (:a_inst %)}) filtered-collab)
                       (map #(identity {:pid (:b_pid %)
                                        :country (:b_country %)
                                        :inst (:b_inst %)}) filtered-collab))))
        inst-country (vec
                      (set
                       (concat
                        (map #(identity {:country  (:a_country %)
                                         :inst (:a_inst %)}) filtered-collab)
                        (map #(identity {:country (:b_country %)
                                         :inst (:b_inst %)}) filtered-collab))))]
    (sort-by
     :label
     (mapv
      (fn [i]
        (merge
         (identity {:id (:inst i)  :label (:inst i)})
         {:authors
          (sort-by
           :label
           (mapv
            (fn [a] (identity {:id (:pid a)  :label (get pid->name (:pid a))}))
            (filter (fn [x] (= (:inst x) (:inst i))) author-inst)))}))
      (filter (fn [x] (= (:country x) (name country-id))) inst-country)))
    #_(mapv
     #(into
       {}
       [{:id
         (util/s->id (-> % first first))
         :label
         (-> % first second)}
        {:countries
         (sort-by
          :label
          (mapv
           (fn [[k _]]
             (merge
              (hash-map :id (util/s->id (first k)) :label (second k))
              {:institutions
               (sort-by
                :label
                (mapv
                 (fn [i]
                   (merge
                    (identity {:id (:inst i)  :label (:inst i)})
                    {:authors
                     (sort-by
                      :label
                      (mapv
                       (fn [a] (identity {:id (:pid a)  :label (get pid->name (:pid a))}))
                       (filter (fn [x] (= (:inst x) (:inst i))) author-inst)))}))
                 (filter (fn [x] (= (:country x) (first k))) inst-country)))}))
           (group-by (juxt :country-id :country-name) (second %))))}])
     (group-by (juxt :region-id :region-name) (filter #(not (or (= (:region-id %) "wd")
                                                                (= (:region-id %) "dach")
                                                                )) selected-region-mapping)))))
    

(defn nested-author-list []
  (let [filtered-collab (subscribe [::db/data-field :get-filtered-collab])
        country-id (subscribe [::db/user-input-field :country-selection])] 
    (when (and @filtered-collab @country-id)
      (let
       [content (nested-authors (name @country-id))
        list-items
        (for [inst content]
          (merge
           {:id (util/s->id (:id inst))
            :costum-label [:div {:style {:width "90%" :display :flex :justify-content :space-between}}
                           [:>  mui-list-item-text {:primary (:label inst)}]
                           [:>  mui-list-item-text {:primary (count (:authors inst)) #_(str "(" (count (:authors inst)) " Authors)")
                                                    :primary-typography-props {:text-align :right}}]]}
           (when (:authors inst)
             {:children
              (for [author (:authors inst)]
                {:id (:id author)
                 :costum-label
                 [horizontal-stack
                  {:stack-args {:spacing 2}
                   :items [[:a {:href (dblp-author-page (:id author))}
                            [:img {:src "img/dblp.png" :target "_blank"
                                   :width 10 :height 10 :padding 10}]]
                           [:> mui-list-item-text {:primary (:label author)}]
                           ]}]})})))] 
        (concat [{:id :institutions
                  :costum-label
                  [:div {:style {:width "90%" :display :flex :justify-content :space-between}}
                   [:>  mui-list-item-text {:primary "Institutions"
                                            :primary-typography-props {:font-size 18}}]
                   [:>  mui-list-item-text {:primary "Author Count"
                                            :primary-typography-props {:text-align :right :font-size 18}}]]}] 
                list-items)))))

(defn update-data []
  (let [config @(subscribe [::common/filter-config])]
    (dispatch [::api/get-filtered-collab config])))

(defn author-view []
  (let [country-id (subscribe [::db/user-input-field :country-selection])
        loading? (subscribe [::db/loading? :get-filtered-collab])
        reset (atom 0)]
    (add-watch loading? ::data-loading
               (fn [_ _ _ data-loading?] 
                 (if data-loading?
                   (dispatch [::feedback/open :data-loading])
                   (dispatch [::feedback/close :data-loading]))))
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
        ^{:key [@loading? @reset]}
        [:div
         [:div {:style {:display :flex :justify-content :space-between :background-color :white}}
          [country-selection]
          [button/update-button
           {:on-click #(do (swap! reset inc) (update-data))}]] 
         [:div {:style {:display :flex :justify-content :left :margin-top 20}}
          ^{:key @country-id}
          [nested-list
           {:id :author-list
            :list-args {:dense false :sx {#_#_:background-color :white :max-width 700 :width "100%"}}
            :content (nested-author-list)}]]]]])))

(comment
  (set (map :region-id @(subscribe [::db/data-field :get-region-mapping])))
  (first @(subscribe [::db/data-field :get-csauthors]))
  @(subscribe [::filter-panel/selected-countries])
  @(subscribe [::filter-panel/selected-regions])
  (nested-authors :ch)
  )