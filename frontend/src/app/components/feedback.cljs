(ns app.components.feedback
  (:require
   [app.util :as util]
   [app.db :as db]
   [reagent.core :as r]
   [re-frame.core :refer (dispatch subscribe reg-event-fx reg-sub)]
   ["@mui/material/Snackbar" :default mui-snackbar]
   [app.components.alert :refer (alert)]))

(reg-event-fx
 ::open
 (fn [{db :db} [_ id]]
   {:db (assoc-in db [:ui-states :feedback/open? id] true)}))

(reg-event-fx
 ::close
 (fn [{db :db} [_ id]]
   {:db (assoc-in db [:ui-states :feedback/open? id] false)}))

(reg-sub
 ::open?
 :<- [::db/ui-states]
 (fn [m [_ id]]
   (get-in m [:feedback/open? id])))

(defn snackbar [{:keys [id props message action content auto-hide-duration ] 
                 :or {auto-hide-duration 3000 }}]
  [:> mui-snackbar
   (util/deep-merge
    {:open @(subscribe [::open? id])
     :on-close #(dispatch [::close id])
     :message (r/as-element message)
     :action action
     :anchor-origin {:vertical :bottom :horizontal :center}
     :auto-hide-duration auto-hide-duration}
    props)
   content])

(defn feedback [{:keys [id props message action status auto-hide-duration]}]
  [snackbar
   {:id id
    :props props
    :action action
    :auto-hide-duration auto-hide-duration
    :content [:div [alert {:severity (or status :info)
                      :on-close #(dispatch [::close id])
                      :content message
                      :alert-args {:sx {:width "100%"}}}]]}])