(ns app.cscollab.interactive-map
  (:require [reagent.core :as reagent :refer [atom]]
            [app.common.leaflet :refer [leaflet]]
            [leaflet :as L]
            [re-frame.core :refer
             (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]))


#_(reg-sub
 ::geometries
 :<- [:app.demo.data/selected-inst]
 (fn [selected-inst]
   (mapv
    #(hash-map
      :type :inst-marker
      :name (:institution %)
      :coordinates [(get-in % [:coord :lat]) (get-in % [:coord :lon])])
    selected-inst)))


(def view-position (atom [49.8 13.1]))
(def zoom-level (atom 6))
(def geometries
  (atom [#_{:type :line
          :coordinates [[45.7 12.8]
                        [40.7 10.8]]}
         #_{:type :marker
          :coordinates [45.7 12.8]}]))



(defn interactive-map [] 
  (fn []
    [leaflet
     {:id "TEST"
      :layers
      [{:type :tile
        :url "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
        :attribution "&copy; <a href=\"http://www.openstreetmap.org/copyright\">OpenStreetMap</a>"}]
      :style {:width "100%" :height "600px"} ;; set width/height as CSS units
      :view view-position ;; map center position
      :zoom zoom-level ;; map zoom level
               ;; Geometry shapes to draw to the map
      :geometries geometries

               ;; Add handler for map clicks
      #_#_:on-click #()}]))

(comment
  (reset! zoom-level 5))