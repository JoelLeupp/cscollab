(ns app.components.motion
  (:require
   ["framer-motion" :refer (motion AnimatePresence useSpring useMotionValue
                                   useTransform useViewportScroll
                                   AnimateSharedLayout) :as motion]
   [cljs-bean.core :refer [bean ->clj ->js]]))
 

(def div
  (.-div motion))

(def circle
  (.-circle motion))

(def rect
  (.-rect motion))

(def span
  (.-span motion))

(def li
  (.-li motion))

(def ul
  (.-ul motion))

(def img
  (.-img motion))

(def button
  (.-button motion))

(def input
  (.-input motion))

(def textarea
  (.-textarea motion))

(def label
  (.-label motion))

(def transform #(motion/transform (->js %1) (->js %2) (->js %3)))

(def animate-presence AnimatePresence)

(def animate-shared-layout AnimateSharedLayout)

(def use-spring useSpring)
 
(def use-motion-value useMotionValue)
 
(def use-transform useTransform)

(def use-viewport-scroll useViewportScroll)

(def spring
  {:type :spring
   :damping 1000
   :stiffness 10700})