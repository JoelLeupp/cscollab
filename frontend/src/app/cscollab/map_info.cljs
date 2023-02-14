(ns app.cscollab.map-info
  (:require [reagent.core :as reagent :refer [atom]]
            [app.common.leaflet :as ll :refer [leaflet-map]]
            [app.cscollab.transformer :as tf]
            [app.cscollab.data :as data]
            [app.components.lists :refer [collapse]]
            [app.db :as db]
            [app.components.button :as button]
            [app.components.stack :refer (horizontal-stack)]
            [reagent-mui.material.paper :refer [paper]]
            [app.components.table :refer (basic-table)]
            [leaflet :as L]
            [re-frame.core :refer
             (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]))



(defn dblp-author-page [pid]
  (str "https://dblp.org/pid/" pid ".html")) 


(defn author-table [author-map collab-count]
  (let [csauthors @(subscribe [::data/csauthors])
        pid->name (zipmap (map :pid csauthors) (map :name csauthors))
        header [{:id :author
                 :label (str "Authors (" (count author-map) ")")}
                {:id :count
                 :label (str "Publications (" collab-count ")") :align :right}]
        author-item (fn [pid]
                      [horizontal-stack
                       {:stack-args {:spacing 2}
                        :items [[:span (get pid->name pid)]
                                [:a {:href (dblp-author-page pid)}
                                 [:img {:src "img/dblp.png" :target "_blank" 
                                        :width 10 :height 10 :padding 10}]]]}])
        author-map (map #(assoc % :author (author-item (:author %))) author-map)]
    (fn []
      [basic-table
       {:header header
        :body author-map
        :paper-args {:sx {:width 340 :display :flex :justify-content :center :margin :auto}}
        :container-args {:sx {:max-height 200}}
        :table-args {:sticky-header true :size :small}}])))

(defn inst-info []
  (let [records (subscribe [::ll/selected-records])
        selected-shape (subscribe [::ll/selected-shape])]
    (fn []
      (when (and @records (string? @selected-shape))
        (let [institution @selected-shape
              collab-count
              (count (set (map :rec_id @records)))
              author-map
              (reverse
               (sort-by
                :count
                (map
                 (fn [[grp-key values]]
                   {:author grp-key
                    :count (count (set (map :rec_id values)))})
                 (merge-with into
                             (group-by :a_pid (filter #(= (:a_inst %) @selected-shape) @records))
                             (group-by :b_pid (filter #(= (:b_inst %) @selected-shape) @records))))))]
          [:div
           [:h4 {:style {:margin-top 0}} institution]
           ^{:key author-map}
           [author-table author-map collab-count]
           ])))))


(defn collab-info []
  (let [records (subscribe [::ll/selected-records])
        selected-shape (subscribe [::ll/selected-shape])]
    (fn []
      (when (and @records (vector? @selected-shape))
            (let [number-collabs (count (set (map :rec_id @records)))]
              [:div
               [:h4  "Collaboration"]
               [:h4
                (first @selected-shape) " And " (second @selected-shape)]
               [:span (str "Number of collaborations: " number-collabs)]])))))

(defn map-info [{:keys [inst?]}]
  (let [selected-shape (subscribe [::ll/selected-shape])
        records (subscribe [::ll/selected-records])]
    (fn []
      ^{:key @records}
      [:div
       (if (string? @selected-shape) 
         [inst-info]
         [collab-info])])))

(defn map-info-div []
  (fn []
    [collapse
     {:sub (subscribe [::ll/info-open?])
      :div
      [:div {:style {:position :absolute :right "10%" :z-index 10}}
       [:div {:style {:background-color :white :height "70vh" :min-width "350px" :padding-left 10 :padding-right 10}}
        [:div {:style {:display :flex :justify-content :space-between}}
         [:h3 "Info Selected"]
         [button/close-button
          {:on-click #(do (dispatch [::ll/set-leaflet [:info-open?] false])
                          (dispatch [::ll/set-leaflet [:selected-shape] nil]))}]]
        [map-info {inst? true}]]]}]))

(comment 
  (let [records @(subscribe [::ll/selected-records])]
    (clojure.set/union
     (set (map #(identity [(:a_inst %) (:b_inst %)]) records)) 
     (set (map :a_inst records))
     (set (map :b_inst records))))
  
  (def records (subscribe [::ll/selected-records]))
  @records
  (def selected-shape (subscribe [::ll/selected-shape]))
  (sort-by
   :count
   (map
    (fn [[grp-key values]]
      {:author grp-key
       :count (count (set (map :rec_id values)))})
    (merge-with into
                (group-by :a_pid (filter #(= (:a_inst %) @selected-shape) @records))
                (group-by :b_pid (filter #(= (:b_inst %) @selected-shape) @records)))))
  
  (count (filter #(= (:a_inst %) @selected-shape) @records))
  (count (filter #(= (:b_inst %) @selected-shape) @records))
  
  
  (fn []
    (when (and @records (string? @selected-shape))
      (let [institution @selected-shape
            collab-count
            (count (set (map :rec_id @records)))
            author-count
            (count
             (vec
              (set
               (flatten
                (map
                 #(concat
                   []
                   (when (= institution (:a_inst %))
                     [(:a_pid %)])
                   (when (= institution (:b_inst %))
                     [(:b_pid %)]))
                 @records)))))]
        [:div
         [:h4 institution]
         [:span (str "Number of authors: " author-count)]
         [:br]
         [:span (str "Number of collaborations: " collab-count)]])))
  )