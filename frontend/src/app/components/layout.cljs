(ns app.components.layout
  (:require
   ["@mui/material/Container" :default mui-container]
   ["@mui/material/Grid" :default mui-grid]
   ["@mui/material/Typography" :default mui-typography]
   [re-frame.core :as rf]
   [app.components.colors :as colors]
   [app.components.motion :as motion]))


(defn grid-template-column []
  "minmax(10px, 10%) 1fr minmax(10px, 10%)")

(defn section [props & body]
  (if-not (map? props)
    [section {} (concat [props] body)]
    [:div
     {:style (merge {:display :grid
                     :grid-template-columns (grid-template-column)
                     :padding-bottom 10
                     :padding-top 10
                     :width "100%"
                     :background-color
                     (case (:color props :main)
                       :dark (colors/bg-colors :dark)
                       :bright (colors/bg-colors :bright)
                       (:light :white) (colors/bg-colors :white)
                       :transparency "rgba(0,0,0,0)"
                       (colors/bg-colors :main))}
                    (:style props))}
     body]))

(defn content
      ([props & body]
       (if (map? props)
         (into
          [:div {:style
                 (merge
                  {:width "100%"
                   :grid-column 2
                   :grid-column-end 2
                   :padding-left 0
                   :padding-right 0}
                  (:style props))}] body)
         (content {} (concat [props] body)))))

(defn title [{:keys [style animate?] :as m
                  :or {animate? true}} & s]

      (let [{:keys [background-color color]} style
            spaces {:margin-top 20
                    :margin-bottom 20
                    :padding-top 10
                    :padding-bottom 20}]
        [:div {:style {:display :grid
                       :grid-column-start 1
                       :grid-column-end 4}}
         [:div
          {:style
           {:display :grid
            :grid-template-columns (grid-template-column)}}
          [:div {:style (merge spaces
                               {:background-color background-color
                                :color color
                                :grid-row-start 2})}]

          [:> mui-typography
           {:variant :h2
            :style (merge spaces
                          {:font-weight :bold
                           :grid-row-start 2
                           :grid-column-start 2
                           :grid-column-end 3
                           :background-color background-color
                           :color color})}
           [:div {:style {:width "100%"}}
            (if animate?
              (for [el s]
                (if (string? el)
                  (for [[i character] (map-indexed vector (vec el))]
                    [:> motion/span {:initial {:opacity 0 :y 50}
                                     :animate {:opacity 1 :y 0}
                                     :transition {:delay (+ (* i 0.03) 0.5)}
                                     :key (str character "-" i)}
                     character])
                  el))
              s)]]]]))

(defn title-bright [& children]
  (into [title {:style {:background-color (colors/bg-colors :bright)}}] 
        children))


(defn title-white [& children]
  (into
   [title {:style {:background-color (colors/bg-colors :white)}}]
   children))


(def title-light title-white)


(defn title-dark [& children]
  (into [title {:style {:background-color (colors/bg-colors :dark)
                        :color (colors/bg-colors :white)}}]
        children))

(comment
  )