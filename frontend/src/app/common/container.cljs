(ns app.common.container
  (:require [reagent.core :as reagent :refer [atom]]
            [app.components.colors :refer [colors]]
            [app.components.lists :refer [collapse]]
            [app.components.colors :refer [colors area-color sub-area-color]]
            [app.db :as db]
            ["@mui/material/Stack" :default mui-stack]
            ["@mui/material/Typography" :default mui-typography]
            [app.components.button :as button]
            [reagent-mui.material.paper :refer [paper]]
            [re-frame.core :refer
             (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
            [app.util :as util]))


(reg-sub
 ::full-screen?
 :<- [::db/ui-states]
 (fn [m  [_ id]]
   (let [id (if (vector? id) id [id])]
     (get-in m (concat id [:full-screen?])))))

(defn open-full-screen [id]
  (dispatch [::db/set-ui-states (conj (if (vector? id) id [id]) :full-screen?) true]))

(defn close-full-screen [id]
  (dispatch [::db/set-ui-states (conj (if (vector? id) id [id]) :full-screen?) false]))


(def area-ids [:ai :systems :theory :interdiscip])
(def sub-area-ids [:ai :ml :vision :nlp :ir :architecture :hpc :security :databases
                   :pl :networks :se :embedded :da :os :mobile+web :metrics :math
                   :hci :vis :robotics :bio :graphics])

(defn legend-div [{:keys [color-by bg-color height]}]
  (let [area-mapping (subscribe [::db/data-field :get-area-mapping])]
    (when (or (= color-by :area) (= color-by :subarea))
      (let
       [area-names
        (vec
         (set (map #(select-keys % [:area-id :area-label :sub-area-id :sub-area-label]) @area-mapping)))
        [ids id label color-map] (if (= color-by :area)
                                   [area-ids :area-id :area-label area-color]
                                   [sub-area-ids :sub-area-id :sub-area-label sub-area-color])
        area-map
        (zipmap (map #(keyword (get % id)) area-names) (map label area-names))]
        [:div {:style {:position :absolute :z-index 10}}
         [:div {:style {:background-color bg-color :height height :display :flex  :align-items :center}} 
          [:> mui-stack {:direction :column :justify-content :center
                         :align-items :flex-start :spacing 0 :sx {:background-color bg-color :padding 1}}
           (map
            #(identity [paper {:elevation 0 :sx {:background-color :transparent}}
                        [:> mui-stack {:direction :row :spacing 1}
                         [:div
                          {:style {:background-color (get color-map %) :width 20 :margin-top 3 :margin-bottom 3}}]
                         [:> mui-typography {:variant :caption :font-size 14 :sx {:margin 0 :padding 0}}
                          (get area-map %)]]]) ids)]]]))))

(defn viz-container [{:keys [id title update-event content info-component info-open? color-by legend-bg-color]}]
  (let [full-screen? (subscribe [::full-screen? id])]
    (fn []
      [paper {:elevation 1 #_#_:sx {:width "100vw" :height "100vh" :position :absolute :top 0 :left 0}}
       [:div {:style {:display :flex :justify-content :space-between :background-color :white
                      :paddin-top 0 :padding-left 20 :padding-right 20 :position :relative}}
        [:h1 {:style {:margin 10}} title]
        [button/update-button
         {:on-click update-event}]]
       (let [[width height] (if @full-screen? ["100vw" "100vh"] ["100%" "70vh"])]
         {:key @full-screen?}
         [:div {:style (merge {:height height :width width} (when @full-screen? {:position :absolute :top 0 :left 0}))}
          [legend-div {:color-by color-by :bg-color legend-bg-color :height height}]
          [collapse
           {:sub info-open?
            :div
            [:div {:style {:position :absolute :right (if @full-screen? 0 "10%") :z-index 10}}
             [:div {:style {:background-color :white :height height :min-width "400px" :padding-left 10 :padding-right 10}}
              [:div {:style {:height "100%"}}
               info-component]]]}]
          [:div {:style {:position :absolute :right (if @full-screen? 0 "10%") :z-index 5}}
           (if @full-screen?
             [button/full-screen-exit-button
              {:on-click #(close-full-screen id)}]
             [button/full-screen-button
              {:on-click #(open-full-screen id)}])]
          content])])))

(comment @(subscribe [::db/ui-states-field [:viz :open?]])
         (dispatch [::db/set-ui-states [:viz :open?] false])
         (dispatch [::db/set-ui-states [:viz :open?] true])
         (subscribe [:app.common.container/full-screen?:map-container])
         @(subscribe [::full-screen? :graph])
         (open-full-screen :graph-container)
         (close-full-screen :graph)
         @(subscribe [::db/ui-states])
         (let [[width height] (if true ["100vw" "100vh"] ["100%" "70vh"])]
           [width height]))