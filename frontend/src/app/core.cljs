(ns app.core
  (:require 
   ["react-dom/client" :refer (createRoot)]
   [app.db :as db]
   [reagent.core :as r]
   [re-frame.core :refer (dispatch dispatch-sync)]
   [app.router :as router :refer (init-routes!)]
   [app.views :refer (app routes)]))

(defonce root (createRoot 
               (.getElementById js/document "app")))

(defn init-app []
  (.render root (r/as-element [app])))

(defn main []
  (println "[main]: loading")
  (dispatch-sync [::db/initialize-db])
  (init-routes! (routes) {:use-fragment true})
  (init-app))

(defn ^:dev/after-load reload! []
  (println "[main]: app reloaded")
  (init-routes! (routes) {:use-fragment true})
  (init-app))

