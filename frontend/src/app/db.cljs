(ns app.db
  (:require
   [re-frame.core :refer
    (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)] 
   [ajax.core :as ajax :refer (json-request-format json-response-format)]))

(def default-db
  ^{:inspect "@re-frame.db/app-db"}
  {:current-route nil
   :ui-states {}
   :user-input {}
   :data {}
   :loading {}
   :errors []})

(reg-event-db
 ::initialize-db
 (fn [_ _] 
   default-db))

(reg-sub
 ::data
 (fn [db _] (:data db)))

(reg-sub
 ::data-field
 :<- [::data]
 (fn [m  [_ id]]
   (let [id (if (vector? id) id [id])]
     (get-in m id))))

(reg-event-fx
 ::set-data
 (fn [{db :db} [_ id value]]
   (let [id (if (vector? id) id [id])]
     {:db (assoc-in db (into [:data] id) value)})))

(reg-event-fx
 ::update-data
 (fn [{db :db} [_ id f]]
   (let [id (if (vector? id) id [id])]
     {:db (update-in db (into [:data] id) f)})))

(reg-sub
 ::user-input
 (fn [db _] (:user-input db)))

(reg-sub
 ::user-input-field
 :<- [::user-input]
 (fn [m  [_ id]]
   (let [id (if (vector? id) id [id])]
     (get-in m id))))

(reg-event-fx
 ::set-user-input
 (fn [{db :db} [_ id value]]
   (let [id (if (vector? id) id [id])]
     {:db (assoc-in db (into [:user-input] id) value)})))

;; takes a collection of [id value] pairs instead
;; enforces transaction property all or nothing
(reg-event-fx
 ::set-user-inputs
 (fn [{db :db} [_ id-vals]]
   {:db (reduce
         (fn [db [id value]]
           (let [id (if (vector? id) id [id])]
             (assoc-in db (into [:user-input] id) value)))
         db id-vals)}))

(reg-event-fx 
 ::set-user-input-selection
 (fn [{db :db} [_ id value add?]]
   (let [id (if (vector? id) id [id])]
     {:db (update-in
           db
           (into [:user-input] id)
           (if (or add? (nil? add?))
             (if (set? value)
               (fnil clojure.set/union #{})
               (fnil conj #{}))
             (if (set? value)
               (fnil clojure.set/difference #{})
               (fnil disj #{})))
           value)})))

(reg-event-fx
 ::update-user-input
 (fn [{db :db} [_ id f]]
   (let [id (if (vector? id) id [id])]
     {:db (update-in db (into [:user-input] id) f)})))

(reg-event-fx
 ::remove-user-input
 (fn [{db :db} [_ id]]
   (let [id (if (vector? id) id [id])]
     {:db (update-in db (into [:user-input] (butlast id))
                     dissoc (last id))})))

(reg-sub
 ::ui-states
 (fn [db _] (:ui-states db)))

(reg-sub
 ::ui-states-field
 :<- [::ui-states]
 (fn [m  [_ id]]
   (let [id (if (vector? id) id [id])]
     (get-in m id))))

(reg-event-fx
 ::set-ui-states
 (fn [{db :db} [_ id value]]
   (let [id (if (vector? id) id [id])]
     {:db (assoc-in db (into [:ui-states] id) value)})))

(reg-event-fx
 ::update-ui-states
 (fn [{db :db} [_ id f]]
   (let [id (if (vector? id) id [id])]
     {:db (update-in db (into [:ui-states] id) f)})))


(comment
  (keys @re-frame.db/app-db)
  (:ui-states (update-in @re-frame.db/app-db [:ui-states :autocomplete-multi] #(disj % :g)))
  (reg-event-fx
   ::clean-data
   (fn [{db :db} _]
     {:db (assoc db :data {})}))
  (dispatch [::clean-data])
  (dispatch [::set-data [:test :a] 1])
  @(subscribe [::data-field :test])
  @(subscribe [::data-field [:test :a]])
  (dispatch [::set-user-input :on? true])
  (dispatch [::set-user-inputs [[:a 1] [:b 2]]])
  (dispatch [::remove-user-input :a])
  (dispatch [::set-user-input-selection :selection :b false])
  @(subscribe [::user-input]))
