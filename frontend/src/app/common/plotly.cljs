(ns app.common.plotly
  (:require [reagent.core :as reagent :refer [atom]] 
            [app.components.colors :as colors] 
            [app.db :as db] 
            ["@mui/material/Box" :default mui-box]
            ["react-plotly.js" :default react-plotly]
            [reagent-mui.material.paper :refer [paper]] 
            [re-frame.core :refer
             (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
            [app.util :as util]))

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


(defn get-area-color [area]
  (get (zipmap
        ["ai" "systems" "theory" "interdiscip"]
        colors/palette) area))

(def test-data
  [{:area-id "ai",
    :area-label "AI",
    :sub-area-id "vision",
    :sub-area-label "Computer Vision",
    :sub-area "vision",
    :count 75}
   {:area-id "ai",
    :area-label "AI",
    :sub-area-id "nlp",
    :sub-area-label "Natural language Processing",
    :sub-area "nlp",
    :count 13}
   {:area-id "ai", :area-label "AI", :sub-area-id "ml", :sub-area-label "Machine Learning", :sub-area "ml", :count 12}
   {:area-id "interdiscip",
    :area-label "Interdisciplinary Areas",
    :sub-area-id "vis",
    :sub-area-label "Visualization",
    :sub-area "vis",
    :count 10}
   {:area-id "interdiscip",
    :area-label "Interdisciplinary Areas",
    :sub-area-id "robotics",
    :sub-area-label "Robotics",
    :sub-area "robotics",
    :count 3}
   {:area-id "ai",
    :area-label "AI",
    :sub-area-id "ai",
    :sub-area-label "Artificial Intelligence",
    :sub-area "ai",
    :count 3}
   {:area-id "systems",
    :area-label "Systems",
    :sub-area-id "se",
    :sub-area-label "Software engineering",
    :sub-area "se",
    :count 3}
   {:area-id "ai",
    :area-label "AI",
    :sub-area-id "ir",
    :sub-area-label "Information Retrieval",
    :sub-area "ir",
    :count 2}
   {:area-id "interdiscip",
    :area-label "Interdisciplinary Areas",
    :sub-area-id "hci",
    :sub-area-label "Human-computer interaction",
    :sub-area "hci",
    :count 2}
   {:area-id "systems",
    :area-label "Systems",
    :sub-area-id "security",
    :sub-area-label "Computer Security and Cryptography",
    :sub-area "security",
    :count 2}
   {:area-id "systems",
    :area-label "Systems",
    :sub-area-id "mobile+web",
    :sub-area-label "Web, Mobile & Multimedia Technologies",
    :sub-area "mobile+web",
    :count 1}
   {:area-id "systems",
    :area-label "Systems",
    :sub-area-id "databases",
    :sub-area-label "Databases & Information Systems",
    :sub-area "databases",
    :count 1}])

(defn get-data [test-data]
  (let [indexed-data (map #(assoc %1 :tickval %2) test-data (range 0 (count test-data)))
        grouped-data (group-by (juxt :area-id :area-label) indexed-data)]
    (vec
     (for [[[area-id area-label] vals] grouped-data]
       {:x (mapv :count vals)
        :y (mapv :tickval vals)
        :name (str area-label " (" (reduce + (map :count vals)) ")")
        :type :bar
        :orientation :h
        :hoverinfo "none"
        :textposition :outside
        :text (mapv #(str (:count %)) vals)
        :transforms [{:type :sort :target :x :order :descending}]
        :marker {:color (get-area-color area-id)}}))))


(defn test-plot [] 
  [plot
   {:box-args {:height "35vh" :width 400 :overflow :auto :margin-top 2}
    :style {:width 380 :height 600}
    :layout {:margin  {:pad 10 :t 0 :b 30 :l 200 :r 5}
             :bargap 0.2
             #_#_:title "Publications per Area"
             :legend {:y 1.15 :x -1
                      :orientation :h}
             :xaxis {:range [0 (+ 10 (apply max (map :count test-data)))]}
             :yaxis {:autorange :reversed
                     :tickmode :array
                     :tickvals (vec (range 0 (count test-data)))
                     :ticktext (mapv #(util/wrap-line (:sub-area-label %) 30) test-data)}}
    :data (get-data test-data)}])


