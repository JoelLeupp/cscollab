(ns app.components.button
  (:require
   ["@mui/material/Button" :default mui-button]
   ["@mui/material/IconButton" :default mui-icon-button]
   ["@mui/icons-material/Update" :default ic-update]
   ["@mui/icons-material/Close" :default ic-close]
   ["@mui/icons-material/Fullscreen" :default ic-full-screen]
   ["@mui/icons-material/FullscreenExit" :default ic-full-screen-exit]
   ["@mui/material/Icon" :default mui-icon]
   ["@mui/material/Stack" :default mui-stack]
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

(defn icon-button 
  [{:keys [event icon] :as m}]
  [:> mui-icon-button
   (util/deep-merge
    {:on-click (when event #(dispatch event))} m) icon])

(defn update-button
  [{:keys [event] :as m}]
  [icon-button (util/deep-merge
                {:event event
                 :icon [:> ic-update]} m)])

(defn close-button
  [{:keys [event] :as m}]
  [icon-button (util/deep-merge
                {:event event
                 :icon [:> ic-close]} m)])

(defn full-screen-button
  [{:keys [event] :as m}]
  [icon-button (util/deep-merge
                {:event event
                 :icon [:> ic-full-screen]} m)])

(defn full-screen-exit-button
  [{:keys [event] :as m}]
  [icon-button (util/deep-merge
                {:event event
                 :icon [:> ic-full-screen-exit]} m)])


(defn costume-icon [src props]
  [:> mui-icon
   [:img (merge {:src src} props)]])

(defn dblp-button
  [{:keys [event] :as m}]
  [icon-button (util/deep-merge
                {:event event
                 :icon [costume-icon "img/dblp.png" 
                        {:width 10 :height 10}]} m)])




