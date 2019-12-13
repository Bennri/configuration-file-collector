(ns configCollector.core
  (:gen-class)
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
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



(defn get-mappings
  ([zip-file-name] (get-mappings zip-file-name "mapping.json"))
  ([zip-file-name mapping-file-name]
  (let [json-mappings-list (f-zip/get-mapping-from-zipped-file zip-file-name mapping-file-name)]
    (map #(Mapping. (:id %1) (:file-name %1) (:source-path %1)) json-mappings-list))))



(defn -main [& args]
  (let* [mapping-file-name "mapping.json"
         app-config (load-app-config "config-data.json")
         file-streams true
         res (create-config-records (:config-files app-config) file-streams)
         zip-file-name (:target-path app-config)]
    (f-zip/create-zip-file zip-file-name mapping-file-name (res 0) (res 1) file-streams)))

;(main)