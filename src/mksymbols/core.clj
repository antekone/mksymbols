(ns mksymbols.core
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

;; https://github.com/clojure/tools.cli
(def cli-options
    [["-n" "--name DIRNAME" "Search for ELF files in this directory"
      :default "."
      :parse-fn #(%)]
    ["-h" "--help"         "Display help (this screen)"]])

(defn -main [& args]
    (let [args (parse-opts args cli-options)]
        (println (args :summary))))

(defn r [] (use 'mksymbols.core :reload))
