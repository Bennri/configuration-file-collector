(ns configCollector.records)

(defrecord ConfigFileStr [id source-path actual-config-file])
(defrecord ConfigFile [id source-path in-stream])
(defrecord Mapping [id file-name source-path])