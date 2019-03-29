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

(ns barometer.protocols
  "Generic clojure protocols which correspond to the interfaces
  provided by Coda Hale
  (https://metrics.dropwizard.io/3.2.0/apidocs/com/codahale/metrics/package-summary.html)"
  (:refer-clojure :exclude [count update time min max]))

(defprotocol ICanExplain
  (explanation [this] "Returns a string containing the documentation for this thing.")
  (explain [this] "Prints a string containing the documentation for this thing."))

(defprotocol MetricSet
  (metrics [ms] "Returns a map name->metric for all metrics contained by this metric set."))

(defprotocol Registrar
  (register [reg name metric] "Registers the given metric to the given name in this registry. If a metric is already registered under this name, it is replaced and a warning is logged.")
  (register-all [reg name->metric] [reg prefix name->metric] "Registers to the given names the corresponding metrics. If a metric is already registered under this name, it is replaced and a warning is logged.")
  (remove-metric [reg name] "Removes from this registry any metric registered under the given name.")
  (remove-matching [reg filter] "Removes from this registry any metric that matches the given filter.")
  (get-metric [reg name] "Retrieves the metric registered under this name. Returns nil if none is found.")
  (names [reg] "Returns a set containing all the names registered in this registry.")
  (gauges [reg] [reg filter] "Returns a map name->metric for all gauges registered in this registry.")
  (meters [reg] [reg filter] "Returns a map name->metric for all meters registered in this registry.")
  (timers [reg] [reg filter] "Returns a map name->metric for all timers registered in this registry.")
  (histograms [reg] [reg filter] "Returns a map name->metric for all histograms registered in this registry.")
  (counters [reg] [reg filter] "Returns a map name->metric for all counters registered in this registry."))

(defprotocol Metered
  (fifteen-minute-rate [metric] "Gives the frequency with which this metric was triggered in the last fifteen minutes.")
  (five-minute-rate [metric] "Gives the frequency with which this metric was triggered in the last five minutes.")
  (mean-rate [metric] "Gives the mean frequency with which this metric has been triggered.")
  (one-minute-rate [metric] "Gives the frequency with which this metric was triggered in the last five minutes."))

(defprotocol Snappable
  (snapshot [metric] "Gives an instantaneous snapshot of the statistical properties of the metric."))

(defprotocol Statistical
  (median [o] "Gives the median value recorded by this metric/snapshot.")
  (mean [o] "Gives the mean value recorded by this metric/snapshot.")
  (size [o] "Gives the number of values recorded by this metric/snapshot.")
  (quantile [o q] "Gives the value recorded by this metric/snapshot in the specified quantile.")
  (values [o] "Gives an array of all values captured by this metric/snapshot.")
  (max [o] "Gives the maximum value captured by this metric/snapshot.")
  (min [o] "Gives the minimum value captured by this metric/snapshot.")
  (std-dev [o] "Gives the standard deviation of the value recorded by this metric/snapshot."))

(defprotocol Counting
  (count [metric] "Returns the count recorded by this metric."))

(defprotocol Counter
  (increment [counter] [counter n] "Increment a counter by 1 or by the amount specified.")
  (decrement [counter] [counter n] "Decrement a counter by 1 or by the amount specified."))

(defprotocol Valuable
  (value [metric] "For metrics that record a single value, this returns it."))

(defprotocol Updateable
  (update [metric] [metric value] "Updates with number of occurrences, or latest value, or whatever it makes sense to tick/mark/update this metric with."))

(defprotocol Timer
  (time [metric fn] "Runs the fn, returns its return value."))
