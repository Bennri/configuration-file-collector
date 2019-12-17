(ns configCollector.core
  (:gen-class)
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [configCollector.records]
            [configCollector.zip-functions :as f-zip])
  (:import (org.apache.commons.io FilenameUtils))
  (:import (configCollector.records ConfigFile))
  (:import (configCollector.records ConfigFileStr))
  (:import (configCollector.records Mapping)))



(defn load-app-config [path]
  (json/read-str (slurp path) :key-fn keyword))


(defn create-config-records
  ([config-path-list] (create-config-records config-path-list true 0))
  ([config-path-list file-stream] (create-config-records config-path-list file-stream 0))
  ([config-path-list file-streams init-id]
   (loop [i init-id paths config-path-list res [] mappings []]
     (if (first paths)
       (recur (+ i 1)
              (rest paths)
              (if file-streams
                (conj res (ConfigFile. i (FilenameUtils/getName (first paths)) (io/input-stream (first paths))))
                (conj res (ConfigFileStr. i (FilenameUtils/getName (first paths)) (slurp (first paths)))))
              (conj mappings (Mapping. i
                                       (FilenameUtils/getName (first paths))
                                       (FilenameUtils/getFullPath (first paths))))) [res mappings]))))



(def cli-options
  [["-c" "--collect-configs VALUE" "Collect configuration files."
    :default "true"
    :parse-fn #(str %)
    :validate [#(or (= % (str false)) (= % (str true))) "Must be true or false."]]
   ["-z" "--zip-file ZIP" "Zip file to extract."
    :default nil
    :parse-fn #(str %)
    :validate [#(= "zip" (last (str/split (str %) #"\."))) "Must be a zip file."]]
   ["-h" "--help"]])


(defn create-summary [summary]
  (str "Usage: \n" summary "\n Note that if -c is set to true (default) -z is ignored. "))



(defn -main [& args]

  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) (println (create-summary summary))
      (Boolean/parseBoolean (:collect-configs options)) (let* [mapping-file-name "mapping.json"
                                                               app-config (load-app-config "config-data.json")
                                                               file-streams true
                                                               res (create-config-records (:config-files app-config) file-streams)
                                                               zip-file-name (:target-path app-config)]
                                                          (f-zip/create-zip-file zip-file-name mapping-file-name (res 0) (res 1) file-streams))
      (and (not (Boolean/parseBoolean (:collect-configs options)))
           (:zip-file options)) (f-zip/place-zipped-files (:zip-file options))
      :else (println (create-summary summary)))))
