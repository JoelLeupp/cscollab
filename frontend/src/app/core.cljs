(ns app.core
  (:require 
   ["react-dom/client" :refer (createRoot)]
   [app.db :as db]
   [reagent.core :as r]
   [day8.re-frame.http-fx] ; add http-xhrio events
   [re-frame.core :refer (dispatch dispatch-sync)]
   [app.router :as router :refer (init-routes!)]
   [app.cscollab.data :as data] 
   [app.views :refer (app routes)]))

(defonce root (createRoot 
               (.getElementById js/document "app")))

(defn init-app []
  (.render root (r/as-element [app])))

(defn main []
  (println "[main]: loading")
  (dispatch-sync [::db/initialize-db])
  (init-routes! (routes) {:use-fragment true})
  (data/get-json-data) ;load static data
  (init-app))

(defn ^:dev/after-load reload! []
  (println "[main]: app reloaded")
  (init-routes! (routes) {:use-fragment true})
  (data/get-json-data) ;load static data
  (init-app))


