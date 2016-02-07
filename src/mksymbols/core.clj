(ns mksymbols.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io   :as    io])

  (:gen-class))

(def glob-dumpsym-path "/home/antek/temp/build/crashes/dump_syms")

;; https://github.com/clojure/tools.cli
(def cli-options [
    ["-n" "--name DIRNAME" "Search for ELF files in this directory"
      :id       :name
      :default  nil]
    ["-s" "--symboldir DIRNAME" "Where to put the symbols?"
      :id       :symdir
      :default  nil]
    ["-h" "--help"         "Display help (this screen)"]])

(defn read-4 [path]
    (let [file (io/file path)]
        (with-open [in (java.io.DataInputStream. (io/input-stream file))]
            (.readInt in))))

(defn is-elf [path] (= 0x7f454c46 (read-4 path)))

(defn do-process-elf [path]
    (let [dumpsym-cmdline (format "%s %s 2> /dev/null >/tmp/%s.sym" glob-dumpsym-path path "basename")]
        (println dumpsym-cmdline)
        true))

(defn process-file [path]
    (if (true? (is-elf path))
        (do-process-elf path)))

(defn process-item [fileobj]
    (let [filepath (.getPath fileobj)]
        (if (true? (.isDirectory fileobj))
            (process-dir filepath)
            (process-file filepath))))

(defn process-dir [path]
    (doseq [fileobj (.listFiles (io/file path))]
        (if (false? (process-item fileobj))
            (printf "Processing item '%s' failed.\n" (.getPath fileobj)))))

(defn run-main [args]
    (let [path   ((args :options) :name)
          symdir ((args :options) :symdir)]
        (if (or (nil? path)
                (nil? symdir))
            (printf "Please use correct -n and -s settings. Use -h to see help.\n")
            (do
                (printf "Symbol store:  '%s'\n" symdir)
                (printf "Entering path: '%s'\n" path)
                (process-dir path)))))

(defn -main [& args]
    (let [args (parse-opts args cli-options)]
        (let [opts (args :options)]
            (if (true? (opts :help))
                (do (println "Help:")
                    (println "")
                    (println (args :summary)))
                (run-main args)))))

(defn r [] (use 'mksymbols.core :reload))

;; vim: set tw=0:
