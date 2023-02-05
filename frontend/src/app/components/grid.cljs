(ns app.components.grid
  (:require
   ["@mui/material/Unstable_Grid2" :default mui-grid]
   ["@mui/material/Box" :default mui-box]
   ["@mui/material/Paper" :default mui-paper]
   ["@mui/material/styles" :as mui-styles]
   [reagent-mui.styles :as styles]
   [reagent-mui.material.paper :refer [paper]]
   [reagent.core :as r] 
   [app.util :as util]))



(defn grid [{:keys [content box-args grid-args item-style item-args]}]
  (let [cosume-styles (fn [{:keys [theme]}] item-style)
        item (styles/styled paper cosume-styles)]
    [:> mui-box {:sx (util/deep-merge {:flex-grow 1} box-args)}
     [:> mui-grid (util/deep-merge {:container true :spacing 2} grid-args)
      (for [c content]
        [:> mui-grid {:xs (:xs c)} 
         [item (util/deep-merge (or item-args {}) (:args c)) 
          (:content c)]])]]))



(defn test-grid []
  [grid {:content [{:xs 8 :content "item with with 8" :args {:elevation 5}}
                   {:xs 4 :content "item with with 4"}]
         :item-style {:text-align :center
                      :color :red}}])


