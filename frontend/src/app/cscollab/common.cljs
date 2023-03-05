(ns app.cscollab.common
  (:require [reagent.core :as reagent :refer [atom]] 
            [app.cscollab.data :as data]
            [app.cscollab.filter-panel :as filter-panel]
            [app.components.colors :refer [colors]] 
            [cljs-bean.core :refer [bean ->clj ->js]]
            [app.db :as db]
            [app.cscollab.map-panel :as mp]
            [re-frame.core :refer
             (dispatch reg-event-fx reg-fx reg-event-db reg-sub subscribe)]
            [app.util :as util]))

(reg-sub
 ::filter-config
 :<- [::mp/insti?]
 :<- [::filter-panel/selected-areas]
 :<- [::filter-panel/selected-sub-areas]
 :<- [::filter-panel/selected-regions]
 :<- [::filter-panel/selected-countries]
 :<- [::filter-panel/selected-year-span]
 :<- [::db/user-input-field [:strict-boundary]]
 (fn [[insti? areas sub-areas regions countries [from-year to-year] strict-boundary]]
   (when (and  areas sub-areas regions countries)
     {"from_year" from-year
      "to_year" to-year
      "area_ids" (mapv name areas)
      "sub_area_ids" (mapv name sub-areas)
      "region_ids" (mapv name regions)
      "country_ids" (mapv name countries)
      "strict_boundary" strict-boundary
      "institution" insti?})))

(comment 
  @(subscribe [::filter-config]))