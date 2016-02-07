(ns mksymbols.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io   :as    io]
            [clojure.java.shell :as   shell])

  (:gen-class))

(def glob-dumpsym-path "/home/antek/temp/build/crashes/dump_syms")
(def glob-symbols-path "/home/antek/temp/build/crashes/symbols")

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

(defn extract-buildid-from-headline [headline]
    (let [match (re-find #".* ([0-9a-fA-F]+) .*" headline)]
        (if (> (count match) 1)
            (match 1)
            nil)))

(defn extract-buildid [symdata]
    (let [headline (.readLine (java.io.BufferedReader. (java.io.StringReader. (.toString symdata))))]
        (extract-buildid-from-headline headline)))

(defn do-move-symdata [args]
    (let [symdata (args :symdata)
          buildid (args :buildid)
          abspath (args :abspath)]
        (let [new-sym-file (java.io.File. (format "%s/%s/%s"
                                                  glob-symbols-path
                                                  (.getFileName abspath)
                                                  buildid))]
            (do
                (if (false? (.isDirectory new-sym-file))
                    (if (false? (.mkdirs new-sym-file))
                        (do
                            (println (format "Can't create directory for %s: %s" (.toString abspath) (.getPath new-sym-file)))
                            false)))
                (do
                    (spit (format "%s/%s/%s/%s.sym" glob-symbols-path (.getFileName abspath) buildid (.getFileName abspath))
                          symdata))))))

(defn do-process-elf [path]
    (let [abspath (.normalize (.toPath (io/file path)))]
        (do
            (println (format "Processing ELF file %s" (.toString abspath)))

            (let [symdata (clojure.java.shell/sh glob-dumpsym-path (.toString abspath))]
                (let [buildid (extract-buildid (symdata :out))]
                    (if (= nil buildid)
                        (do
                            (println (format "Can't process ELF: %s" path))
                            false)
                        (do-move-symdata {:symdata (symdata :out)
                                          :buildid buildid
                                          :abspath abspath})))))))

(defn process-file [path]
    (if (true? (is-elf path))
        (do-process-elf path)))

(declare process-dir)

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
            (do
                (println "Please use correct -n and -s settings. Use -h to see help.")
                1)
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
                        (println (args :summary))))
                (run-main args))))

(defn r [] (use 'mksymbols.core :reload))

;; vim: set tw=0:
