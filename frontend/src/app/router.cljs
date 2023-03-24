(ns app.router
  "Defines functionalities for navigation"
  (:require
   [re-frame.core :refer
    (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
   [reitit.frontend.easy :as rfe]
   [reitit.core :as r]))

;; defines router and routing events and subsriptions

(reg-sub
 ::current-route
 (fn [db _]
   (:current-route db)))

(reg-sub
 ::current-path
 :<- [::current-route]
 (fn [match]
   (:path match)))

(def href rfe/href)

(reg-fx 
 ::navigate!
 (fn [route]
   (apply rfe/push-state route)))

(reg-event-fx
 ::navigate
 (fn [{db :db} [_ & route]]
   {:db db
    ::navigate! route}))

(reg-event-db
 ::navigated 
 (fn [db [_ new-match]]
   (assoc db :current-route new-match)))

(defn on-navigate [new-match]
  (when new-match
    (dispatch [::navigated new-match])))

(defn router [routes]
  (r/router routes))

(defn init-routes!
  ([routes] (init-routes! routes {:use-fragment false}))
  ([routes opts] 
   (js/console.log "initializing routes")
   (rfe/start! (router routes) on-navigate opts)))

(comment
  @(subscribe [::current-route]) 
  )



