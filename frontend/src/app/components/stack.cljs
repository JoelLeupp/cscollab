(ns app.components.stack
  (:require
   [reagent-mui.util]
   ["@mui/material/Box" :default mui-box]
   ["@mui/material/Typography" :default mui-typography]
   ["@mui/material/Stack" :default mui-stack]
   ["@mui/material/InputLabel" :default mui-input-label] 
   [app.util :as util]
   [app.db :as db]
   [reagent.core :as r]
   [re-frame.core :refer (dispatch subscribe)]))


(defn stack [{:keys [items stack-args box-args]}]
  [:> mui-box {:sx (util/deep-merge {:width "100%"} box-args)}
   [:> mui-stack stack-args
    (for [i items]
      i)]])


(defn horizontal-stack [{:keys [items stack-args box-args]}]
  [stack {:items items
          :box-args box-args
          :stack-args (util/deep-merge {:spacing 1 :direction :row} stack-args)}])