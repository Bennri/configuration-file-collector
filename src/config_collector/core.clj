(ns config-collector.core
  (:gen-class)
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io])
  (:import (java.util.zip ZipOutputStream ZipEntry ZipFile))
  (:import (org.apache.commons.io FilenameUtils IOUtils)))


(defrecord ConfigFileStr [id source-path actual-config-file])
(defrecord ConfigFile [id source-path in-stream])
(defrecord Mapping [id file-name source-path])


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


(defn create-zip-entry-from-str [zip file-id ^String content]
  (doto zip
    (.putNextEntry (ZipEntry. file-id))
    (.write (.getBytes content))
    (.closeEntry)))


(defn create-zip-entry-from-file [zip file-id in-stream]
  (doto zip
    (.putNextEntry (ZipEntry. file-id))
    (.write (IOUtils/toByteArray in-stream))
    (.closeEntry)))


(defn create-zip-file [^String zip-file-name
                       ^String mapping-file-name config-records-list mappings-list file-streams]
  (with-open [file (io/output-stream zip-file-name)
              zip (ZipOutputStream. file)
              wrt (io/writer zip)]
    (binding [*out* wrt]
      (let [entry-creator-file (partial create-zip-entry-from-file zip)
            entry-creator-str (partial create-zip-entry-from-str zip)]
        (loop [config-records config-records-list]
          (when (first config-records)
            (if file-streams
              (entry-creator-file (str (:id (first config-records))) (:in-stream (first config-records)))
              (entry-creator-str (str (:id (first config-records))) (:actual-config-file (first config-records))))
            (recur (rest config-records))))
        (entry-creator-str mapping-file-name (json/write-str mappings-list))))))




(defn get-zipped-files-print [^String zip-file-name]
  (with-open [zip-file (ZipFile. zip-file-name)]
    (for [zip-entry (doall (->> (.entries zip-file) (enumeration-seq)))]
      (do
        (println (IOUtils/toString (.getInputStream zip-file zip-entry)))
        (println (.getName zip-entry))))) )



(defn get-mapping-from-zipped-file
  ([^String zip-file-name] (get-mapping-from-zipped-file zip-file-name "mapping.json"))
  ([^String zip-file-name ^String mapping-file-name]
   (json/read-str
     (with-open [zip-file (ZipFile. zip-file-name)]
       (IOUtils/toString
         (.getInputStream zip-file
                          (first (filter #(= (.getName %1) mapping-file-name)
                                         (doall (->> (.entries zip-file) ;; doall to evaluate the lazy sequence immediately
                                                     (enumeration-seq) vec))))))) :key-fn keyword)))


(defn get-mappings
  ([zip-file-name] (get-mappings zip-file-name "mapping.json"))
  ([zip-file-name mapping-file-name]
  (let [json-mappings-list (get-mapping-from-zipped-file zip-file-name mapping-file-name)]
    (map #(Mapping. (:id %1) (:file-name %1) (:source-path %1)) json-mappings-list))))



(defn -main [& args]
  (let* [
         mapping-file-name "mapping.json"
         app-config (load-app-config "resources/config-data.json")
         file-streams true
         res (create-config-records (:config-files app-config) file-streams)
         zip-file-name (:target-path app-config)]
    (create-zip-file zip-file-name mapping-file-name (res 0) (res 1) file-streams)))

;(main)