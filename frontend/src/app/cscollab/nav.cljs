(ns app.cscollab.nav
  (:require
   [app.components.app-bar :refer (app-bar)]))



(defn nav-bar []
  [app-bar {:app-name "Visualization of Scientific Collaboration Networks"
            :right-div [:div {:style {:display :flex :align-items :center}}
                        "University of Zürich"]}])

