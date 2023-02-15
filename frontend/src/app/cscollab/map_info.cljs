(ns app.cscollab.map-info
  (:require [reagent.core :as reagent :refer [atom]]
            [app.common.leaflet :as ll :refer [leaflet-map]]
            [app.cscollab.transformer :as tf]
            [app.common.plotly :as plotly]
            [app.cscollab.data :as data]
            [app.util :as util]
            [app.components.lists :refer [collapse]]
            [app.db :as db]
            [app.components.button :as button]
            [app.components.stack :refer (horizontal-stack)]
            [reagent-mui.material.paper :refer [paper]]
            [app.components.table :refer (basic-table)]
            [leaflet :as L]
            [re-frame.core :refer
             (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]))



(defn dblp-author-page [pid]
  (str "https://dblp.org/pid/" pid ".html")) 


(defn author-table [author-map collab-count]
  (let [csauthors @(subscribe [::data/csauthors])
        pid->name (zipmap (map :pid csauthors) (map :name csauthors))
        header [{:id :author
                 :label [:b (str "Authors (" (count author-map) ")")]}
                {:id :count
                 :label [:b (str "Publications (" collab-count ")")] :align :right}]
        author-item (fn [pid]
                      [horizontal-stack
                       {:stack-args {:spacing 2}
                        :items [[:span (get pid->name pid)]
                                [:a {:href (dblp-author-page pid)}
                                 [:img {:src "img/dblp.png" :target "_blank" 
                                        :width 10 :height 10 :padding 10}]]
                                [:div {:style {:margin-top 1}}
                                 [:img {:src "img/scholar-favicon.ico" :target "_blank"
                                        :width 10 :height 10}]]]}])
        author-map (map #(assoc % :author (author-item (:author %))) author-map)]
    (fn []
      [basic-table
       {:header header
        :body author-map
        :paper-args {:sx {:width "100%" :display :flex :justify-content :center :margin :auto} :elevation 0}
        :container-args {:sx {:max-height "22vh"}}
        :table-args {:sticky-header true :size :small}}])))

(defn get-plot-data [data]
  (let [indexed-data (map #(assoc %1 :tickval %2) data (range 0 (count data)))
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
        :marker {:color (plotly/get-area-color area-id)}}))))

(defn publication-plot []
  (let [records (subscribe [::ll/selected-records])
        area-mapping (subscribe [::data/area-mapping])]
    (fn []
      (let
       [area-names
        (vec
         (set (map #(select-keys % [:area-id :area-label :sub-area-id :sub-area-label]) @area-mapping)))
        sub-area-map
        (zipmap (map :sub-area-id area-names) area-names)
        sub-area-count
        (reverse
         (sort-by
          :count
          (map
           (fn [[grp-key values]]
             {:sub-area grp-key
              :count (count (set (map :rec_id values)))})
           (group-by :rec_sub_area @records))))
        area-data
        (map #(let [sub-area-info (get sub-area-map (:sub-area %))]
                (merge sub-area-info %)) sub-area-count)
        plot-data (get-plot-data area-data)]
        [plotly/plot
         {:box-args {:height "36vh" :width 460 :overflow :auto :margin-top 2}
          :style {:width 440 :height (+ 100 (* 35 (count area-data)))}
          :layout {:margin  {:pad 10 :t 0 :b 30 :l 200 :r 5}
                   :bargap 0.2
                   #_#_:title "Publications per Area"
                   :legend {:y 1.1 :x -1
                            :orientation :h}
                   :xaxis {:range [0 (+ 25 (apply max (map :count area-data)))]}
                   :yaxis {:autorange :reversed
                           :tickmode :array
                           :tickvals (vec (range 0 (count area-data)))
                           :ticktext (mapv #(util/wrap-line (:sub-area-label %) 30) area-data)}}
          :data plot-data}]))))

(defn inst-info []
  (let [records (subscribe [::ll/selected-records])
        selected-shape (subscribe [::ll/selected-shape])]
    (fn []
      (when (and @records (string? @selected-shape))
        (let [institution @selected-shape
              collab-count
              (count (set (map :rec_id @records)))
              author-map
              (reverse
               (sort-by
                :count
                (map
                 (fn [[grp-key values]]
                   {:author grp-key
                    :count (count (set (map :rec_id values)))})
                 (merge-with into
                             (group-by :a_pid (filter #(= (:a_inst %) @selected-shape) @records))
                             (group-by :b_pid (filter #(= (:b_inst %) @selected-shape) @records))))))]
          [:div
           [:div {:style {:display :flex :justify-content :space-between}}
            [:h3 #_{:style {:margin-top 0}} institution]
            [button/close-button
             {:on-click #(do (dispatch [::ll/set-leaflet [:info-open?] false])
                             (dispatch [::ll/set-leaflet [:selected-shape] nil]))}]]
           ^{:key author-map}
           [:div
            [author-table author-map collab-count]
            [publication-plot]]
           ])))))

(defn publication-plot-collab []
  (let [records (subscribe [::ll/selected-records])
        area-mapping (subscribe [::data/area-mapping])]
    (fn []
      (let
       [area-names
        (vec
         (set (map #(select-keys % [:area-id :area-label :sub-area-id :sub-area-label]) @area-mapping)))
        sub-area-map
        (zipmap (map :sub-area-id area-names) area-names)
        sub-area-count
        (reverse
         (sort-by
          :count
          (map
           (fn [[grp-key values]]
             {:sub-area grp-key
              :count (count (set (map :rec_id values)))})
           (group-by :rec_sub_area @records))))
        area-data
        (map #(let [sub-area-info (get sub-area-map (:sub-area %))]
                (merge sub-area-info %)) sub-area-count)
        plot-data (get-plot-data area-data)]
        [plotly/plot
         {:box-args {:height "50vh" :width 460 :overflow :auto :margin-top 2}
          :style {:width 440 :height (max 300 (+ 100 (* 35 (count area-data))))}
          :layout {:margin  {:pad 10 :t 80 :b 30 :l 200 :r 5}
                   :annotations [{:xref :paper :yref :paper :xanchor :left :yanchor :top :x 0 :y 1.3
                                  :xshift -175 :yshift 10 :text "<b>Publications by Area</b>"
                                  :align :left :showarrow false :font {:size 18}}]
                   :bargap 0.2
                   #_#_:title "Publications per Area"
                   :legend {:y 1.2 :x -1
                            :orientation :h}
                   :xaxis {:range [0 (+ 25 (apply max (map :count area-data)))]}
                   :yaxis {:autorange :reversed
                           :tickmode :array
                           :tickvals (vec (range 0 (count area-data)))
                           :ticktext (mapv #(util/wrap-line (:sub-area-label %) 30) area-data)}}
          :data plot-data}]))))

(defn collab-info []
  (let [records (subscribe [::ll/selected-records])
        selected-shape (subscribe [::ll/selected-shape])]
    (fn []
      (when (and @records (vector? @selected-shape))
            (let [number-collabs (count (set (map :rec_id @records)))]
              [:div
               [:div {:style {:display :flex :justify-content :space-between}}
                [:h3  "Collaboration"]
                [button/close-button
                 {:on-click #(do (dispatch [::ll/set-leaflet [:info-open?] false])
                                 (dispatch [::ll/set-leaflet [:selected-shape] nil]))}]] 
               [:h3
                (first @selected-shape) " And " (second @selected-shape)]
               [publication-plot-collab]])))))

(defn map-info [{:keys [insti?]}]
  (let [selected-shape (subscribe [::ll/selected-shape])
        geometries (subscribe [::ll/geometries])] 
    (fn [] 
      ^{:keys @geometries}
      [:div
       (if (string? @selected-shape) 
         [inst-info]
         [collab-info])])))

(defn map-info-div [] 
   (let [geometries (subscribe [::ll/geometries])] 
     (fn []
       ^{:key @geometries}
       [collapse
        {:sub (subscribe [::ll/info-open?])
         :div
         [:div {:style {:position :absolute :right "10%" :z-index 10}}
          [:div {:style {:background-color :white :height "70vh" :min-width "400px" :padding-left 10 :padding-right 10}}
           #_[:div {:style {:display :flex :justify-content :space-between}}
              #_[:h3 "Info Selected"]
              [button/close-button
               {:on-click #(do (dispatch [::ll/set-leaflet [:info-open?] false])
                               (dispatch [::ll/set-leaflet [:selected-shape] nil]))}]] 
           [map-info {:insti? true}]]]}])))

(comment 
  (let [records @(subscribe [::ll/selected-records])]
    (clojure.set/union
     (set (map #(identity [(:a_inst %) (:b_inst %)]) records)) 
     (set (map :a_inst records))
     (set (map :b_inst records))))
  
  (def records @(subscribe [::ll/selected-records]))
  (def area-mapping @(subscribe [::data/area-mapping]))
  
  (def area-names
    (vec
     (set (map #(select-keys % [:area-id :area-label :sub-area-id :sub-area-label]) area-mapping))))
  (def sub-area-map 
    (zipmap (map :sub-area-id area-names) area-names))
  (get sub-area-map "networks")
  
  (def selected-shape (subscribe [::ll/selected-shape]))
  (def sub-area-count
    (reverse
     (sort-by
      :count
      (map
       (fn [[grp-key values]]
         {:sub-area grp-key
          :count (count (set (map :rec_id values)))})
       (group-by :rec_sub_area records)))))
  
  (map #(let [sub-area-info (get sub-area-map (:sub-area %))]
          (merge sub-area-info %)) sub-area-count)
  
  (count (filter #(= (:a_inst %) @selected-shape) @records))
  (count (filter #(= (:b_inst %) @selected-shape) @records))
  
  
  (fn []
    (when (and @records (string? @selected-shape))
      (let [institution @selected-shape
            collab-count
            (count (set (map :rec_id @records)))
            author-count
            (count
             (vec
              (set
               (flatten
                (map
                 #(concat
                   []
                   (when (= institution (:a_inst %))
                     [(:a_pid %)])
                   (when (= institution (:b_inst %))
                     [(:b_pid %)]))
                 @records)))))]
        [:div
         [:h4 institution]
         [:span (str "Number of authors: " author-count)]
         [:br]
         [:span (str "Number of collaborations: " collab-count)]])))
  )