(ns app.common.container
  (:require [reagent.core :as reagent :refer [atom]] 
            [app.components.colors :refer [colors]]
            [app.components.lists :refer [collapse]]
            [app.db :as db] 
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

(defn viz-container [{:keys [id title update-event content info-component info-open?]}]
  (let [full-screen? (subscribe [::full-screen? id])]
    (fn []
      [paper {:elevation 1 #_#_:sx {:width "100vw" :height "100vh" :position :absolute :top 0 :left 0}}
       [:div {:style {:display :flex :justify-content :space-between :background-color :white
                      :paddin-top 0 :padding-left 20 :padding-right 20}}
        [:h1 {:style {:margin 10}} title]
        [button/update-button
         {:on-click update-event}]]
       (let [[width height] (if @full-screen? ["100vw" "100vh"] ["100%" "70vh"])]
         {:key @full-screen?}
         [:div {:style (merge {:height height :width width} (when @full-screen? {:position :absolute :top 0 :left 0}))}
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
           [width height])
         )