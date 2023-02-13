(ns app.cscollab.map-info
  (:require [reagent.core :as reagent :refer [atom]]
            [app.common.leaflet :as ll :refer [leaflet-map]]
            [app.cscollab.transformer :as tf]
            [app.cscollab.data :as data]
            [app.components.lists :refer [collapse]]
            [app.db :as db]
            [app.components.button :as button]
            [reagent-mui.material.paper :refer [paper]]
            [leaflet :as L]
            [re-frame.core :refer
             (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]))



(defn inst-info []
  (let [records (subscribe [::ll/selected-records])
        selected-shape (subscribe [::ll/selected-shape])]
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
           [:span (str "Number of collaborations: " collab-count)]])))))


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
  (let [selected-shape (subscribe [::ll/selected-shape])]
    (fn []
      (if (string? @selected-shape)
        [inst-info]
        [collab-info]))))

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
  )