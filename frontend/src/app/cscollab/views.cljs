(ns app.cscollab.views
  (:require
   [app.components.layout :as acl]
   [app.components.colors :as c]
   [app.components.inputs :as i]
   [app.common.user-input :refer (input-panel)]
   [re-frame.core :refer (dispatch subscribe)]
   ["react-lorem-ipsum" :refer (loremIpsum)]
   [app.components.lists :as lists]
   [app.db :as db]
   [app.cscollab.filter-panel :refer (filter-panel)] 
   [reagent.core :as r]
   [app.cscollab.interactive-map :as interactive-map]
   [app.cscollab.conferences :refer (conferences-view)]
   [app.cscollab.map-panel :refer (map-config-panel)]
   [app.components.table :refer (test-table)]
   [app.common.plotly :as pp :refer (test-plot)]
   [app.cscollab.transformer :as tf]))


(defn main-view []
  (fn []
    [:<> 
     [acl/section
      #_[acl/title-white "Landscape of Scientific Collaborations"]
      [acl/content 
       [filter-panel] 
       [map-config-panel]
       [interactive-map/interactive-map]
       #_[tf/collab-count]]]]))


 
  (defn circle-coord [degree radius]
    [(* radius (js/Math.sin degree)) (* radius (js/Math.cos degree))])


(def nodes (mapv #(hash-map :id %) (range 0 50)))

(defn concentrated-circle [nodes]
  (loop [new-nodes []
         nodes-left nodes
         n 0]
    (if (empty? nodes-left)
      new-nodes
      (let [n-nodes (min (count nodes-left) (* 6 (js/Math.pow 2 n)))
            nodes-taken (subvec nodes-left 0 n-nodes)
            radius (* (+ 1 n) 0.5)
            degree (/ (* 2 js/Math.PI) n-nodes)
            node-coord
            (map
             (fn [node t]
               (let [d (+ (* degree t) (when-not (= 0 (mod n 2)) (/ degree 2)))
                     x (* radius (js/Math.sin d))
                     y (* radius (js/Math.cos d))]
                 (merge node {:x x :y y})))
             nodes-taken (range 0 n-nodes))] 
        (recur
         (vec (concat new-nodes node-coord))
         (subvec nodes-left n-nodes)
         (inc n))))))

(def my-nodes (concentrated-circle nodes))

(defn circle-plot []
  [:> pp/plotly
   {:style {:width 500 :height 500}
    :data [{:x (mapv :x my-nodes)
            :y (mapv :y my-nodes)
            :mode :markers
            :type :scatter}]}])

(defn conferences []
  (fn []
    [:<>
     [acl/section
      [acl/title-white "Computer Science Conferences"] 
      [acl/content 
       [circle-plot]
       #_[conferences-view]]]]))

