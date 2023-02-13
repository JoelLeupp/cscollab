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

(def test-data
  [{:id 1 :name "abac"}
   {:id 2 :name "abac"}
   {:id 3 :name "abac"}
   {:id 4 :name "abac"}
   {:id 5 :name "abac"}
   {:id 6 :name "abac"}
   {:id 7 :name "abac"}
   {:id 8 :name "abac"}
   {:id 9 :name "abac"}
   {:id 10 :name "abac"}
   {:id 11 :name "abac"}
   {:id 12 :name "abac"}])

(defn simple-table []
  [:> mui-paper {:elevation 1 :sx {:background-color :white :width 200}}
   [:> mui-table-container {:sx {:max-height 200}}
    [:> mui-table {:sticky-header true :size :small}
     [:> mui-table-head
      [:> mui-table-row
       [:> mui-table-cell {:key "author"} "Author"]
       [:> mui-table-cell {key "count" :align :right} "Count"]]]
     [:> mui-table-body
      (for [{:keys [id name]} test-data]
        [:> mui-table-row
         [:> mui-table-cell id]
         [:> mui-table-cell {:align :right} name]])]]]])


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

(def header [{:id :id :label "Author"}
             {:id :name :label "Count" :align :right}])

(defn test-table []
  [basic-table
   {:header header 
    :body test-data
    :paper-args {:sx {:width 200}}
    :container-args {:sx {:max-height 200}}
    :table-args {:sticky-header true :size :small}}])

