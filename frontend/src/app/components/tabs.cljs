(ns app.components.tabs
  (:require
   [app.components.colors :as colors]
   ["@mui/material/Tab" :default mui-tab]
   ["@mui/material/Tabs" :default mui-tabs]
   ["@mui/material/styles" :refer (styled)]
   [cljs-bean.core :refer (->js)]
   [emotion.core :refer (defstyled)]
   ["@mui/material/Box" :default mui-box]
   [app.util :as util]
   [app.db :as db]
   [reagent.core :as r]
   [re-frame.core :refer
    (dispatch  reg-event-db reg-sub subscribe)]))

(reg-event-db
 ::set-tab
 (fn [db [_ [id tab]]]

   (assoc-in db [:ui-states :tabs id] tab)))

(reg-sub
 ::tab
 :<- [::db/ui-states]
 (fn [m [_ id]] (get-in m [:tabs id])))


(defn tabs-comp [{:keys [id choices tab-comp tabs-comp box-args tabs-args tab-args]
                  :or {tab-comp mui-tab tabs-comp mui-tabs}}]
  (let [tab (subscribe [::tab id])]
    (when-not @tab
      (dispatch [::set-tab [id (-> choices first :value)]]))
    [:> mui-box {:sx (util/deep-merge
                      {:width "100%" :border-bottom 3 :border-color :devider
                       :margin-bottom "5px"}
                      box-args)}
     [:> tabs-comp
      (util/deep-merge
       {:value @tab
        :indicatorColor nil
        :on-change (fn [_ tab] 
                     (dispatch [::set-tab [id (keyword tab)]]))}
       tabs-args)
      (for [c choices]
        ^{:key (:value c)}
        [:> tab-comp (util/deep-merge (merge c {:style {:padding 10 :min-width 120}}) tab-args)])]]))


(defn tab-style [type]
  {:color (colors/colors type)
   :opacity 1.0
   :margin-left 0
   :margin-right 3
   :border-radius 0
   :font-weight 600
   :background-color :white
   :indicator {:color :red :opacity 0}
   "&.Mui-selected" {:background-color (colors/colors type)
                     :border-radius 0
                     :color :white}})

(defstyled styled-tab-main
  [mui-tab {:class-name-prop :className
            #_#_:wrap r/adapt-react-class}]
  (tab-style :main))

(defstyled styled-tab-sub
  [mui-tab {:class-name-prop :className
            #_#_:wrap r/adapt-react-class}]
  (tab-style :second))

(defn main-tab [{:keys [id choices box-args tabs-args tab-args]
                 :as m}]
  (tabs-comp (util/deep-merge
              {:tab-comp styled-tab-main
               :box-args {:border-color (colors/colors :main)}}
              m)))

(defn sub-tab [{:keys [id choices box-args tabs-args tab-args]
                :as m}]
  (tabs-comp (util/deep-merge
              {:tab-comp styled-tab-sub
               :box-args {:border-color (colors/colors :main)}}
              m)))