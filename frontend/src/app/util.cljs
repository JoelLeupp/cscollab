(ns app.util
  (:require
   [cljs.reader]))


(defn s->id [s]
  (if (js/Number.isNaN (js/parseInt s))
    (keyword s)
    (js/parseInt s)))

(defn parse-json-string [s]
  (js->clj
   (.parse js/JSON s)
   :keywordize-keys true))


(defn filter-by-mask [coll mask]
  (filter some? (map #(when %1 %2) mask coll)))


(defn deep-merge
  [a b]
  (if (map? a)
    (into a (for [[k v] b] [k (deep-merge (a k) v)]))
    b))


(defn
  ^{:comment "convert a json string to a cljs map"
    :test #(do (assert (= {:a 1} (json->edn "{\"a\":1}")))

               (assert (= {"a" 1} (json->edn "{\"a\":1}" :keywordize false))))}
  json->edn
  [json & {:keys [keywordize] :or {keywordize true}}]
  (js->clj
   (.parse js/JSON json)
   :keywordize-keys keywordize))

 
(defn vec-maps->map-vec [data]
  (let [helper-fun (fn [vec-map] (cond-> vec-map (not (coll? vec-map)) vector))]
    (apply merge-with #(conj (helper-fun %1) %2) data)))


(defn map-vec->vec-maps [data]
  (let [values (vec (vals data))
        key     (keys data)]
    (map #(zipmap key %) (apply map  vector values))))


(defn map-function-on-map-vals [m f]
  (apply merge
         (map (fn [[k v]] {k (f v)})
              m)))


(defn factor-out-key [data key]
  (let [maps data #_(map #(dissoc % key) data)
        map-keys (map #(key %) data)]
    (zipmap (map #(if (int? %) % (keyword %)) map-keys) maps)))


(defn any->boolean [x]
  (cond (= x 0) false
        (= x []) false
        (= x {}) false
        (= x #{}) false
        (= x '()) false
        (= x false) false
        (nil? x) false
        :else true))

(comment
  (test json->edn))

