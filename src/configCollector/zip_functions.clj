(ns configCollector.zip-functions
  (:gen-class)
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io])
  (:import (java.util.zip ZipOutputStream ZipEntry ZipFile)
           (org.apache.commons.io IOUtils)
           (java.io FileOutputStream File)
           (java.nio.file Paths)
           (configCollector.records Mapping)))



(defn create-zip-entry-from-file [zip file-id in-stream]
  (doto zip
    (.putNextEntry (ZipEntry. file-id))
    (.write (IOUtils/toByteArray in-stream))
    (.closeEntry)))


(defn create-zip-entry-from-str [zip file-id ^String content]
  (doto zip
    (.putNextEntry (ZipEntry. file-id))
    (.write (.getBytes content))
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
        (println (.getName zip-entry))))))



(defn get-mapping-from-zipped-file
  ([^String zip-file-name] (get-mapping-from-zipped-file zip-file-name "mapping.json"))
  ([^String zip-file-name ^String mapping-file-name]
   (json/read-str
     (with-open [zip-file (ZipFile. zip-file-name)]
       (IOUtils/toString
         (.getInputStream zip-file
                          (first (filter #(= (.getName %1) mapping-file-name)
                                         (->> (.entries zip-file) ;; doall to evaluate the lazy sequence immediately
                                              (enumeration-seq) vec)))))) :key-fn keyword)))

(defn get-mappings
  ([zip-file-name] (get-mappings zip-file-name "mapping.json"))
  ([zip-file-name mapping-file-name]
   (let [json-mappings-list (get-mapping-from-zipped-file zip-file-name mapping-file-name)]
     (map #(Mapping. (:id %1) (:file-name %1) (:source-path %1)) json-mappings-list))))



;; read from input stream (zip entry) to output stream which is a file placed at the directory from
;; which the file was originally collected
(defn get-zipped-files
  ([^String zip-file-name] (get-zipped-files zip-file-name "mapping.json"))
  ([^String zip-file-name mapping-file-name]
   (let [mappings (get-mappings zip-file-name)]
     (with-open [zip-file (ZipFile. zip-file-name)]
       ;; doall to evaluate the lazy sequence immediately
       (doall
         (for [zip-entry (doall (->> (.entries zip-file) (enumeration-seq)))]
           (when-not (= (.getName zip-entry) mapping-file-name)
             (let [mapping-entry
                   (first (filter #(= (Long/parseLong (.getName zip-entry)) (:id %1)) mappings))
                   file-name (:file-name mapping-entry)
                   path (:source-path mapping-entry)]
               (IOUtils/copy
                 (.getInputStream zip-file zip-entry)
                 (FileOutputStream. (File. (str path file-name))))))))))))
