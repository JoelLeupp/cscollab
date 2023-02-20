(ns app.common.graph
  (:require
   ["cytoscape" :as cy]
   ["cytoscape-dagre" :as dagre]
   ["cytoscape-fcose" :as fcose]
   [app.util :as util]
   [re-frame.core :as rf :refer
    (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
   [app.db :as db]
   ["react-cytoscapejs" :as rcy]))


(reg-sub
 ::graph
 (fn [db _] (:graph db)))

(reg-sub
 ::graph-field
 :<- [::graph]
 (fn [m  [_ id]]
   (let [id (if (vector? id) id [id])]
     (get-in m id))))

(reg-event-fx
 ::set-graph-field
 (fn [{db :db} [_ id value]]
   (let [id (if (vector? id) id [id])]
     {:db (assoc-in db (into [:graph] id) value)})))

(reg-sub
 ::info-open?
 :<- [::graph-field :info-open?]
 (fn [m] (when m m)))

(reg-sub
 ::elements
 :<- [::graph-field :elements]
 (fn [g] (when g g)))

(defn init-cytoscape-dagre []
  (.use cy dagre))

(defn init-cytoscape-fcose []
  (.use cy fcose))

(defn graph [{:keys [elements layout style stylesheet on-click cy]}]
  (let [cy (or cy (atom nil))
        loaded? (atom false)]
    (fn [{:keys [id elements layout style stylesheet on-click]}] 
      [:> rcy
       {:cy
        #(do
           (reset! cy %)
           (dispatch [::set-graph-field [:cy] @cy])
           (when id
             (dispatch [::db/set-ui-states [:graph id] %]))
           (when-not @loaded?
             (reset! loaded? true)
             (.on % "tap" "node" (fn [event] (when on-click (on-click event))))
             (.on % "tap" "edge" (fn [event] (when on-click (on-click event))))
             (.on % "tap" (fn [e]
                            (when (= (.-target e) %)
                              (dispatch [::set-graph-field [:info-open?] false]))))))
        :elements elements
        :layout layout
        :style (util/deep-merge
                {:width "100%" :height "100%" :background-color "white"}
                style)
        :stylesheet stylesheet}])))