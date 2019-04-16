# Barometer

This provides a thin wrapper over Coda Hale's [metrics library for the JVM](https://metrics.dropwizard.io). It provides idiomatic constructors for [`Registry`](src/barometer/core.clj#L49) and all the individual metrics (e.g., [`Timer`](src/barometer/core.clj#L108), [`Meter`](src/barometer/core.clj#L85), [`Gauge`](src/barometer/core.clj#L127), etc.). Several [protocols](src/barometer/protocols.clj) ([`Statistical`](src/barometer/core.clj#L173), [`Metered`](src/barometer/core.clj#L193), [`Counting`](src/barometer/core.clj#L248), etc.) are extended onto the codahale objects to represent semantically-equivalent access patterns analogous to the original interfaces. A [constructor is also provided](src/barometer/core.clj#L348) for the codahale ConsoleReporter for debugging and so forth.

The preservation of common interfaces is the primary difference between barometer and [metrics-clojure](https://github.com/metrics-clojure/metrics-clojure).

Besides the thin wrapper, `barometer.core` does not provide much. The primary novelty introduced is a strict requirement that every metric be accompanied by documentation at its construction (although no effort is made to filter out empty strings). The documentation can be printed/examined by calling protocol methods [`explain`](src/barometer/protocols.clj#L23)/[`explanation`](src/barometer/protocols.clj#L22) on the metric. This protocol is also extended onto the Registry, which gives an explanation for all metrics registered at that time.

A simple example of creating and registering a metric:

```clojure
(def my-registry (m/registry "This registry holds all my metrics."))
(def my-timer (m/timer "This timer times my operation."))
(m/register my-registry :my-lib.package.my-timer my-timer)
```

A simple example of using a timer:

```clojure
(m/with-timer the-best-timer
   (println "Test"))
```

## Notable features

### `(barometer.core/default-registry)`

A default (initially empty) registry available to all consumers of barometer s.t. the single registry could potentially be shared across libraries.

### `barometer.core/with-timer`

Simple convenience macro that times the execution of the body, even if an exception is thrown. Timer must be supplied.

### `barometer.aspects/timed`

Designed to work with [`morphe`](https://github.com/Workiva/morphe), this allows simple generation of timers attached to functions.

```clojure
(ns my-ns
  (:require [[morphe.core :as m]
             [barometer.aspects :as ma]])

(m/defn ^{::d/aspects [ma/timed]} my-fn
   [x y z]
   ...
   )
```

This will create a timer and register it under the default barometer registry under the name `"my-ns.my-fn.timer"`, with the explanation `"Timer for the function: my-ns/my-fn"`.

### `barometer.aspects/concurrency-measured`

Also designed to work with [`morphe`](https://github.com/Workiva/morphe), this allows a way to measure the number of simultaneous in-flight calls to a method.

```clojure
(ns my-ns
  (:require [[morphe.core :as m]
             [barometer.aspects :as ma]])

(m/defn ^{::d/aspects [ma/concurrency-measured]} my-fn
   [x y z]
   ...
   )

```

This will create a counter and register it under the default barometer registry under the name `"my-ns.my-fn.concurrency-counter"`, with the explanation `"Concurrency counter for the function: my-ns/my-fn"`.

# Maintainers and Contributors

## Active Maintainers

-

## Previous Contributors

- Timothy Dean <galdre@gmail.com>
- Houston King <houston.king@workiva.com>
- Aleksandr Furmanov <aleksandr.furmanov@workiva.com>
- Ryan Heimbuch <ryan.heimbuch@workiva.com>
