(defproject com.workiva/barometer "0.1.0"
  :description "A thin wrapper over Coda Hale's metrics library for the JVM"
  :url "https://github.com/Workiva/barometer"
  :license {:name "Apache License, Version 2.0"}

  :plugins [[lein-shell "0.5.0"]
            [lein-cljfmt "0.6.4"]
            [lein-codox "0.10.3"]]

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.workiva/utiliva "0.1.0"]
                 [com.workiva/recide "1.0.0"]
                 [com.workiva/morphe "1.0.0"]
                 [org.clojure/tools.logging "0.4.0"]
                 [io.dropwizard.metrics/metrics-core "3.2.6"]
                 [io.dropwizard.metrics/metrics-graphite "3.2.6"]
                 [io.dropwizard.metrics/metrics-jvm "3.2.6"]]

  :deploy-repositories {"clojars"
                        {:url "https://repo.clojars.org"
                         :username :env/clojars_username
                         :password :env/clojars_password
                         :sign-releases false}}

  :source-paths      ["src"]
  :test-paths        ["test"]

  :global-vars {*warn-on-reflection* true}

  :aliases {"docs" ["do" "clean-docs," "with-profile" "docs" "codox"]
            "clean-docs" ["shell" "rm" "-rf" "./documentation"]}

  :codox {:metadata {:doc/format :markdown}
          :themes [:rdash]
          :html {:transforms [[:title]
                              [:substitute [:title "Barometer API Docs"]]
                              [:span.project-version]
                              [:substitute nil]
                              [:pre.deps]
                              [:substitute [:a {:href "https://clojars.org/com.workiva/barometer"}
                                            [:img {:src "https://img.shields.io/clojars/v/com.workiva/barometer.svg"}]]]]}
          :output-path "documentation"}

  :profiles {:dev [{:dependencies [[criterium "0.4.3"]]}]
             :docs {:dependencies [[codox-theme-rdash "0.1.2"]]}})
