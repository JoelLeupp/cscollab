(ns app.components.table
  (:require
   [app.util :as util]
   [app.db :as db]
   [reagent.core :as r]
   [re-frame.core :refer (dispatch subscribe)]
   ["@mui/material/Table" :default mui-table]
   ["@mui/material/TableBody" :default mui-table-body]
   ["@mui/material/TableCell" :default mui-table-cell]
   ["@mui/material/TableContainer" :default mui-table-container]
   ["@mui/material/TableHead" :default mui-table-head]
   ["@mui/material/TableRow" :default mui-table-row]
   ["@mui/material/Paper" :default mui-paper]))


(defn basic-table [{:keys [header body paper-args container-args table-args]}]
  [:> mui-paper
   (util/deep-merge
    {:elevation 1 :sx {:background-color :white}} paper-args)
   [:> mui-table-container container-args
    [:> mui-table table-args
     [:> mui-table-head
      [:> mui-table-row
       (for [column header]
         [:> mui-table-cell {:key (:id column)
                             :align (or (:align column) :left)
                             :style (:style column)}
          (:label column)])]]
     [:> mui-table-body
      (for [row body]
        [:> mui-table-row
         (for [column header]
           [:> mui-table-cell {:align (or (:align column) :left)}
            (get row (:id column))])])]]]])



