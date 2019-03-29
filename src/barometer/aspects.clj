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

(ns barometer.aspects
  "Provides aspects for use with Morphe (https://github.com/Workiva/morphe)"
  (:require [barometer.core :as core]
            [barometer.protocols :as p]
            [clojure.string :as str]
            [morphe.core :as m])
  (:import [java.util.regex Pattern]))

(def ^:private replacements
  {"?" "_QMARK_"
   "!" "_BANG_"})

(def ^:private replacement-pattern
  (->> (keys replacements)
       (map #(Pattern/quote %))
       (interpose "|")
       (apply str)
       (re-pattern)))

(defn- clean-name
  [name]
  (str/replace name replacement-pattern replacements))

(defn timed
  "Morphe aspect which puts a timer metric into local scope and wraps each fn body with a call to metrics/with-timer."
  [fn-form]
  (let [timer-name (gensym 'timer)]
    (-> fn-form
        (m/alter-form
         `(let [~timer-name (core/timer ~(format "Timer for the function: %s/%s" (ns-name &ns) &name))]
            (p/register core/DEFAULT [~(clean-name (str &ns)) ~(clean-name (str &name)) "timer"] ~timer-name)
            ~&form))
        (m/alter-bodies
         `(core/with-timer ~timer-name ~@&body)))))

(defn concurrency-measured
  "Morphe aspect which puts a counter into local scope and modifies each fn arity to track the number
  of in-flight calls to the function."
  [fn-form]
  (let [counter-name (gensym 'counter)]
    (-> fn-form
        (m/alter-form
         `(let [~counter-name (core/counter ~(format "Concurrency counter for the function: %s/%s" (ns-name &ns) &name))]
            (p/register core/DEFAULT [~(clean-name (str &ns)) ~(clean-name (str &name)) "concurrency-counter"] ~counter-name)
            ~&form))
        (m/alter-bodies
         `(try (p/increment ~counter-name) ;; Fine so long as incrementing doesn't break
               ~@&body
               (finally (p/decrement ~counter-name)))))))
