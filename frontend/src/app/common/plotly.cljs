(ns app.common.plotly
  (:require [reagent.core :as reagent :refer [atom]] 
            [app.components.colors :as colors] 
            [app.db :as db] 
            ["@mui/material/Box" :default mui-box]
            ["react-plotly.js" :default react-plotly] 
            [re-frame.core :refer
             (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
            [app.util :as util]))

;; defines a plotly component and defines default configurations

(def plotly react-plotly)

(def default-config {:displayModeBar false :responsive true :revision 1})

(def default-bar-layout
  {:xaxis {:zerolinewidth 2}
   :yaxis {:showgrid false
           :ticksuffix "  "}
   :bargap 0.2
   :showlegend true
   :legend {:orientation :h}
   :colorway colors/palette})


(defn plot [{:keys [data layout config style box-args]}]
  [:> mui-box {:sx box-args}
   [:> plotly
    {:layout (util/deep-merge default-bar-layout layout)
     :data data
     :useResizeHandler true
     :style (util/deep-merge {:width "100%" :height "100%"} style)
     :config (util/deep-merge default-config config)}]])





