(ns app.common.graph
  (:require
   ["cytoscape" :as cy]
   ["cytoscape-dagre" :as dagre]
   ["cytoscape-fcose" :as fcose]
   [app.util :as util]
   [re-frame.core :refer (dispatch subscribe)]
   [app.db :as db]
   ["react-cytoscapejs" :as rcy]))

(defn init-cytoscape-dagre []
  (.use cy dagre))

(defn init-cytoscape-fcose []
  (.use cy fcose))

(defn graph [{:keys [elements layout style stylesheet on-click cy]}]
  (fn [{:keys [id elements layout style stylesheet on-click]}]
    (let [cy (or cy (atom nil))
          loaded? (atom false)]
      [:> rcy
       {:cy
        #(do
           (reset! cy %)
           (when id
             (dispatch [::db/set-ui-states [:graph id] %]))
           (when-not @loaded?
             (reset! loaded? true)
             (.on % "tap" "node" (fn [event] (when on-click (on-click event))))
             (.on % "tap" (fn [e]
                            (when (= (.-target e) %)
                              (dispatch [::db/set-ui-states [:graph :detail?] false]))))))
        :elements elements
        :layout layout
        :style (util/deep-merge
                {:width "100%" :height "100%" :background-color "white"}
                style)
        :stylesheet stylesheet}])))