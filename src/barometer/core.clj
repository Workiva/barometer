;; Copyright 2016-2019 Workiva Inc.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns barometer.core
  "This is a pretty thin wrapper of Coda Hale
  (https://metrics.dropwizard.io).  Unlike the metrics-clojure library
  (https://github.com/metrics-clojure/metrics-clojure), this doesn't
  destroy your ability to program to interfaces common to the
  different kinds of metrics.

  The one major constraint this wrapper adds is that you must supply a
  docstring when you create metrics. Do it. Don't cheat. We don't need
  anonymous mysterious metrics out there. If it's worth recording,
  it's worth saying what it is!!

  On that note, you can get explanations for each metric with
  (explanation) and (explain)."
  (:require [barometer.protocols :as p]
            [utiliva.core :refer [map-keys]]
            [utiliva.macros :refer [when-class class-sym?]]
            [recide.core :refer [insist]]
            [clojure.tools.logging :as log])
  (:import [com.codahale.metrics MetricRegistry
            Histogram
            Counter
            Gauge
            Timer
            Timer$Context
            Meter
            Metric
            MetricFilter
            ExponentiallyDecayingReservoir]
           [java.util.concurrent Callable])
  (:refer-clojure :exclude [count update time min max]))


;;;;;;;;;;;;;;;;;;
;; CONSTRUCTORS ;;
;;;;;;;;;;;;;;;;;;


(defn registry
  "The explanation is optional."
  ([explanation]
   (proxy [MetricRegistry barometer.protocols.ICanExplain]
          []
     (explanation [] ;; WARNING: Imperative procedural fragile code!!!!!
       (->> (p/metrics this)
            (sort-by key)
            (map (juxt key (comp p/explanation val)))
            (map #(apply str (interpose ": " %)))
            (cons "*********")
            (cons explanation)
            (interpose "\n")
            (apply str)))
     (explain []
       (println (p/explanation this)))))
  ([]
   (proxy [MetricRegistry barometer.protocols.ICanExplain]
          []
     (explanation []
       (->> (p/metrics this)
            (sort-by key)
            (map (juxt key (comp p/explanation val)))
            (map #(apply str (interpose ": " %)))
            (cons "*********")
            (interpose "\n")
            (apply str)))
     (explain []
       (println (p/explanation this))))))

(defn reservoir
  ([] (ExponentiallyDecayingReservoir.)))

(defn meter
  "Creates a codahale meter [https://metrics.dropwizard.io/3.2.0/apidocs/com/codahale/metrics/Meter.html]
  with the provided explanation string."
  ([explanation]
   (proxy [Meter barometer.protocols.ICanExplain]
          []
     (explanation [] explanation)
     (explain [] (println explanation))))
  ([clock explanation]
   (proxy [Meter barometer.protocols.ICanExplain]
          [clock]
     (explanation [] explanation)
     (explain [] (println explanation)))))

(defn histogram
  "Creates a codahale histogram [https://metrics.dropwizard.io/3.2.0/apidocs/com/codahale/metrics/Histogram.html
  with the provided explanation string."
  [reservoir explanation]
  (proxy [Histogram barometer.protocols.ICanExplain]
         [reservoir]
    (explanation [] explanation)
    (explain [] (println explanation))))

(defn timer
  "Creates a codahale timer [https://metrics.dropwizard.io/3.2.0/apidocs/com/codahale/metrics/Timer.html]
  with the provided explanation string."
  ([explanation]
   (proxy [Timer barometer.protocols.ICanExplain]
          []
     (explanation [] explanation)
     (explain [] (println explanation))))
  ([reservoir explanation]
   (proxy [Timer barometer.protocols.ICanExplain]
          [reservoir]
     (explanation [] explanation)
     (explain [] (println explanation))))
  ([reservoir clock explanation]
   (proxy [Timer barometer.protocols.ICanExplain]
          [reservoir clock]
     (explanation [] explanation)
     (explain [] (println explanation)))))

(defn gauge
  "Creates a codahale gauge [https://metrics.dropwizard.io/3.2.0/apidocs/com/codahale/metrics/Gauge.html]
  with the provided explanation string."
  [fn explanation]
  (reify Gauge
    (getValue [this] (fn))
    p/ICanExplain
    (explanation [_] explanation)
    (explain [_] (println explanation))))

(defn counter
  "Creates a codahale counter [https://metrics.dropwizard.io/3.2.0/apidocs/com/codahale/metrics/Counter.html]
  with the provided explanation string."
  [explanation]
  (proxy [Counter barometer.protocols.ICanExplain]
         []
    (explanation [] explanation)
    (explain [] (println explanation))))

(defn metric-filter
  "Pass in a function that takes arguments name-of-metric and metric. It should
  return a truthy value if the filter matches."
  [f]
  (reify MetricFilter
    (matches [_ name metric]
      (boolean (f name metric)))))

(defn ensure-name
  "Metric names can be specified as either a dotted string or a vector of strings."
  [name-repr]
  (cond (string? name-repr)
        name-repr
        (symbol? name-repr)
        (str name-repr)
        (vector? name-repr)
        (MetricRegistry/name ^String (first name-repr) ^"[Ljava.lang.String;" (into-array String (next name-repr)))
        :else (throw (IllegalArgumentException. "Metric names can be specified as either a dotted string or a vector of strings"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Extend the protocols onto codahale metric types ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(extend-protocol p/Snappable
  com.codahale.metrics.Sampling
  (snapshot [metric] (.getSnapshot metric)))

(extend-protocol p/Statistical
  com.codahale.metrics.Snapshot
  (median [o] (.getMedian o))
  (mean [o] (.getMean o))
  (size [o] (.size o))
  (quantile [o x] (.getValue o x))
  (values [o] (.getValues o))
  (max [o] (.getMax o))
  (min [o] (.getMin o))
  (std-dev [o] (.getStdDev o))
  com.codahale.metrics.Sampling
  (median [m] (p/median (p/snapshot m)))
  (mean [m] (p/mean (p/snapshot m)))
  (size [m] (p/size (p/snapshot m)))
  (quantile [m x] (p/quantile (p/snapshot m) x))
  (values [m] (p/values (p/snapshot m)))
  (max [m] (p/max (p/snapshot m)))
  (min [m] (p/min (p/snapshot m)))
  (std-dev [m] (p/std-dev (p/snapshot m))))

(extend-protocol p/Metered
  com.codahale.metrics.Metered
  (fifteen-minute-rate [metric] (.getFifteenMinuteRate metric))
  (five-minute-rate [metric] (.getFiveMinuteRate metric))
  (one-minute-rate [metric] (.getOneMinuteRate metric))
  (mean-rate [metric] (.getMeanRate metric)))

(extend-protocol p/MetricSet
  com.codahale.metrics.MetricSet
  (metrics [ms] (.getMetrics ms)))

(defn- warn-on-replace
  [name]
  (log/warn (str "MetricRegistry already contains a metric by the name '" name "'. Replacing!")))

(extend-protocol p/Registrar
  MetricRegistry
  (register [reg name metric]
    (let [name (ensure-name name)]
      (when (contains? (p/metrics reg) name)
        (.remove reg name)
        (warn-on-replace name))
      (.register reg name metric)))
  (register-all [reg name->metric]
    (let [name->metric (into {} (map-keys ensure-name) name->metric)
          pre-existing (clojure.set/intersection (into #{} (keys name->metric))
                                                 (into #{} (keys (p/metrics reg))))]
      (doseq [name pre-existing]
        (.remove reg name)
        (warn-on-replace name))
      (.registerAll reg
                    (reify com.codahale.metrics.MetricSet
                      (getMetrics [_] name->metric)))))
  (remove-metric [reg name] (.remove reg (ensure-name name)))
  (remove-matching [reg filter] (.removeMatching reg filter))
  (get-metric [reg name]
    (get (p/metrics reg)
         (ensure-name name)))
  (names [reg] (.getNames reg))
  (gauges
    ([reg] (.getGauges reg))
    ([reg filter] (.getGauges reg filter)))
  (meters
    ([reg] (.getMeters reg))
    ([reg filter] (.getMeters reg filter)))
  (timers
    ([reg] (.getTimers reg))
    ([reg filter] (.getTimers reg filter)))
  (histograms
    ([reg] (.getHistograms reg))
    ([reg filter] (.getHistograms reg filter)))
  (counters
    ([reg] (.getCounters reg))
    ([reg filter] (.getCounters reg filter))))

(extend-protocol p/Counting
  com.codahale.metrics.Counting
  (count [metric] (.getCount metric)))

(extend-protocol p/Valuable
  Gauge
  (value [gauge] (.getValue gauge)))

(extend-protocol p/Updateable
  Meter
  (update
    ([metric] (.mark metric))
    ([metric value] (.mark metric (long value))))
  Histogram
  (update
    ([metric] (throw (UnsupportedOperationException. "Histogram can only be updated with explicit new values")))
    ([metric value] (.update metric (long value))))
  Counter
  (update
    ([metric] (throw (UnsupportedOperationException. "Counter can only be updated by passing the desired number to add.")))
    ([metric value] (p/increment metric value)))
  Timer
  (update
    ([metric] (throw (UnsupportedOperationException. "Timer can only be updated by passing in the most recent measurement in ns.")))
    ([metric value] (.update metric value java.util.concurrent.TimeUnit/NANOSECONDS))))

(extend-protocol p/Counter
  Counter
  (increment
    ([counter] (.inc counter))
    ([counter n] (.inc counter n)))
  (decrement
    ([counter] (.dec counter))
    ([counter n] (.dec counter n))))

(extend-protocol p/Timer
  Timer
  (time [timer fn] (.time ^Timer timer ^Callable fn)))

;;;;;;;;;;;;;;;
;; UTILITIES ;;
;;;;;;;;;;;;;;;

(defmacro get-or-register
  "If a metric is already registered under this name, get-or-register simply returns that metric.
  Otherwise, 'construction-form' is evaluated and its result (a metric, presumably) is registered.
  Example: (def my-metric (get-or-register registry 'my-name (meter \"A meter!\")))"
  [reg name construction-form]
  `(let [name# (ensure-name ~name)]
     (if-let [metric# (get (p/metrics ~reg) name#)]
       metric#
       (let [metric# ~construction-form]
         (p/register ~reg name# metric#)
         metric#))))

(defmacro with-timer
  "Times the execution of the body, even if an exception is thrown."
  [timer & body]
  `(do (insist (instance? Timer ~timer))
       (let [context# (.time ~(vary-meta timer assoc :tag 'com.codahale.metrics.Timer))]
         (try ~@body
              (finally
                (.stop ^Timer$Context context#))))))

(defn vomit-timer
  "Named in accordance with the sense that this is not how things should be done
  if we happened to take the time to decide how to do such things instead of simply
  doing them.

  Returns a map of stats reported by the timer."
  [timer]
  (let [snap (p/snapshot timer)
        rate (p/mean-rate timer)]
    {:mean (p/mean snap)
     :median (p/median snap)
     :size (p/size snap)
     :quantile-9 (p/quantile snap 0.9)
     :quantile-99 (p/quantile snap 0.99)
     :quantile-999 (p/quantile snap 0.999)
     :min (p/min snap)
     :max (p/max snap)
     :std-dev (p/std-dev snap)
     :mean-rate rate}))

(defn vomit-all-timers
  "Named in accordance with the sense that this is not how things should be done
  if we happened to take the time to decide how to do such things instead of simply
  doing them.

  Returns a nested map of stats reported by all timers registered in the registry."
  [registry]
  (let [timers (p/timers registry)]
    (reduce (fn [m timer]
              (assoc m
                     (key timer)
                     (-> timer val vomit-timer)))
            (sorted-map)
            timers)))

;;;;;;;;;;;;;;;
;; REPORTERS ;;
;;;;;;;;;;;;;;;

(defn console-reporter
  "A simple console reporter for debugging/dev purposes."
  [registry]
  (.build (com.codahale.metrics.ConsoleReporter/forRegistry registry)))

(defn graphite-reporter
  "A reporter for streaming metrics to a graphite server. http://graphiteapp.org"
  ([registry graphite-url graphite-port] (graphite-reporter registry graphite-url graphite-port ""))
  ([registry graphite-url graphite-port prefix]
   (let [addr (new java.net.InetSocketAddress graphite-url graphite-port)
         graphite (new com.codahale.metrics.graphite.Graphite addr)
         builder (com.codahale.metrics.graphite.GraphiteReporter/forRegistry registry)]
     (doto builder
       (.prefixedWith prefix)
       (.convertRatesTo java.util.concurrent.TimeUnit/SECONDS)
       (.convertDurationsTo java.util.concurrent.TimeUnit/MILLISECONDS)
       (.filter MetricFilter/ALL))
     (.build builder graphite))))

;;;;;;;;;;;;;;;;;;;;;;
;; DEFAULT REGISTRY ;;
;;;;;;;;;;;;;;;;;;;;;;

(defonce DEFAULT
  (registry "DEFAULT REGISTRY: Global metrics for all users of barometer"))

(defn default-registry "Default registry available for use by all consumers of barometer" [] DEFAULT)

(defmacro ^:private def-and-copy-docstrings
  [symbols]
  `(do
     ~@(->> (for [s symbols]
              [`(def ~s ~(symbol "barometer.protocols" (str s)))
               `(alter-meta! (var ~s) merge (-> (var ~(symbol "barometer.protocols" (str s)))
                                                meta
                                                (select-keys [:doc :arglists])))])
            (apply concat))))

(def-and-copy-docstrings ;; import from barometer.protocols
  [explanation
   explain
   metrics
   register
   register-all
   remove-metric
   remove-matching
   get-metric
   names
   gauges
   meters
   timers
   histograms
   counters
   fifteen-minute-rate
   five-minute-rate
   mean-rate
   one-minute-rate
   snapshot
   median
   mean
   size
   quantile
   values
   max
   min
   std-dev
   count
   increment
   decrement
   value
   update
   time])
