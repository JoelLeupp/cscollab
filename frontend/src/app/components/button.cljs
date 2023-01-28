(ns app.components.button
  (:require 
   ["@mui/material/Button" :default mui-button]
   [reagent.core :as r]
   [emotion.core :refer (defstyled)] 
   [app.util :as util]
   [re-frame.core :refer 
    (dispatch subscribe)]))

(defn button
  [{:keys [event text] :as m}]
  [:> mui-button
   (util/deep-merge
    {:variant :contained
     :on-click (when event #(dispatch event))} m)
   text])
