(ns app.components.inputs
  (:require-macros [reagent-mui.util :refer [react-component]])
  (:require
   [reagent-mui.util]
   ["@mui/material/Box" :default mui-box]
   ["@mui/material/Typography" :default mui-typography]
   ["@mui/material/Stack" :default mui-stack]
   ["@mui/material/InputLabel" :default mui-input-label]
   ["@mui/material/MenuItem" :default mui-menu-item]
   ["@mui/material/FormControl" :default mui-form-control] 
   ["@mui/material/FormControlLabel" :default mui-form-control-label]
   ["@mui/material/FormLabel" :default mui-form-label] 
   ["@mui/material/Select" :default mui-select]
   ["@mui/material/Autocomplete" :default mui-auto-complete] 
   ["@mui/material/TextField" :default mui-text-field]
   ["@mui/material/Checkbox" :default mui-checkbox]
   ["@mui/material/Radio" :default mui-radio]
   ["@mui/material/RadioGroup" :default mui-radio-group] 
   ["@mui/material/Switch" :default mui-switch]
   ["@mui/material/Slider" :default mui-slider]
   ["@mui/material/InputAdornment" :default mui-input-adornment]
   ["@mui/icons-material/Search" :default ic-search]
   [app.util :as util]
   [app.db :as db]
   [reagent.core :as r]
   [re-frame.core :refer (dispatch subscribe)]))


(defn select [{:keys [:form-args :select-args :label :label-id :on-change :value :id :choices]}]
  [:> mui-form-control (util/deep-merge {:variant :outlined #_:standard
                                         :style {:min-width 120}} form-args)
   (when label [:> mui-input-label {:id label-id} label])
   [:> mui-select
    (util/deep-merge
     {:label-id label-id 
      :value (or value @(subscribe [::db/user-input-field id]))
      :label label
      :on-change (or on-change (fn [event]
                                 (dispatch [::db/set-user-input id
                                            (util/s->id (-> event .-target .-value))])))}
     select-args)
    (for [{:keys [:value :label]} choices]
      ^{:key value}
      [:> mui-menu-item {:value value} label])]])

(defn radio [{:keys [:form-args :radio-group-args :label :label-id :on-change :default-value :id :choices]}]
  (when (and  (nil? @(subscribe [::db/user-input-field id])) default-value) 
    (dispatch [::db/set-user-input id default-value]))
  [:> mui-form-control  form-args
   (when label [:> mui-form-label {:id label-id} label])
   [:> mui-radio-group
    (util/deep-merge
     {:aria-labelledby label-id 
      :value  @(subscribe [::db/user-input-field id])
      :on-change (or on-change (fn [event]
                                 (dispatch [::db/set-user-input id
                                            (util/s->id (-> event .-target .-value))])))}
     radio-group-args)
    (for [{:keys [:value :label]} choices]
      ^{:key value}
      [:> mui-form-control-label {:value value :control (r/as-element [:> mui-radio]) :label label}])]])

(defn switch [{:keys [:stack-args :typo-args :switch-args :label-off :label-on :id]}]
  [:> mui-stack (util/deep-merge {:spacing 1 :direction :row :align-items :center} stack-args)
   (when label-off [:> mui-typography typo-args label-off])
   [:> mui-switch (util/deep-merge
                   {:checked @(subscribe [::db/user-input-field id])
                    :on-change (fn [event]
                                 (dispatch [::db/set-user-input id
                                            (-> event .-target .-checked)]))}
                   switch-args)]
   (when label-on [:> mui-typography typo-args label-on])])

(defn textfield [{:keys [:id :label :args :multiline? :style :helper-text]}]
  [:> mui-text-field (util/deep-merge
                      {:style style
                       :label label
                       :helper-text helper-text
                       :multiline multiline?
                       :value @(subscribe [::db/user-input-field id])
                       :on-change (fn [event]
                                    (dispatch [::db/set-user-input id
                                               (-> event .-target .-value)]))}
                      args)])

(defn search [{:keys [:id :args :style]}]
  [textfield 
   {:id id
    :style style
    :args (util/deep-merge 
           {:placeholder "Search"
            :Input-props
            {:start-adornment
             (r/as-element
              [:> mui-input-adornment {:position :start}
               [:> ic-search]])}} 
                           args)}])

(defn slider [{:keys [:id :args :step :min :max]}]
  [:> mui-slider
   (util/deep-merge
    {:step step
     :min min
     :max max
     :value @(subscribe [::db/user-input-field id])
     :on-change (fn [event]
                  (dispatch [::db/set-user-input id
                             (-> event .-target .-value)]))}
    args)])

(defn autocomplete [{:keys [:id :label  :options :multiple? :style :args :input-props :option-label]}]
  (let [value
        (subscribe [::db/user-input-field id])
        on-change
        (fn [event v]
          (js/console.log v)
          (dispatch [::db/set-user-input id
                     (if multiple?
                       (into
                        #{}
                        (map #(util/s->id (:value %)))
                        (js->clj v :keywordize-keys true))
                       (util/s->id (:value (js->clj v :keywordize-keys true))))]))
        render-option
        (react-component
         [props option]
         [:li props
          (when multiple?
            [:> mui-checkbox
             {:checked (contains? @value  (util/s->id (:value option)))
              :on-change (fn [e]
                           (js/console.log (clj->js (util/s->id (:value option))))
                           (dispatch [::db/set-user-input-selection id 
                                      (util/s->id (:value option))
                                      (-> e .-target .-checked)]))}])
          (or (when option-label (option-label option)) (:label option))])
        render-input
        (react-component
         [props]
         [:> mui-text-field
          (merge props
                 {:label label
                  :variant :outlined #_:standard}
                 input-props)])]
    (fn [{:keys [:id :options :multiple? :style :args]}] 
      [:> mui-auto-complete
       (util/deep-merge
        {:id id
         :value (if multiple?
                  (apply vector (filter #(contains? @value (:value %)) options))
                  (when @value (first (filter #(= @value (:value %)) options))))
         :multiple multiple?
         :limitTags 3
         :options options
         :style style
         :on-change on-change
         :render-option render-option
         :render-input render-input}
                  args)])))




(comment 
  @(subscribe [::db/user-input])
  #_(fn [^js props option]
      (set! (.-component props) "li")
      (r/create-element
       mui-box props
       (r/as-element
        [:> mui-checkbox
         {:checked (contains? @v-atom (keyword (.-value option))) #_(get @state (keyword (.-value option)))
          :on-change (fn [e]
                       #_(swap! state assoc (keyword (.-value option)) (not (get @state (keyword (.-value option)) false)))
                       (if (not (-> e .-target .-checked))
                         (swap! v-atom disj (keyword (.-value option)))
                         (swap! v-atom conj (keyword (.-value option))))
                       (js/console.log (-> e .-target .-checked)))}])
       (.-label option)
       (r/as-element [:strong {:href "#"}
                      (str "-" (.-label option) (.-value option))])))
  )
