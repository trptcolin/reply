(ns reply.version
  (:import java.util.Properties))

(defn map-from-property-filepath [file] 
  (try 
    (let [file-reader (.. (Thread/currentThread)
                          (getContextClassLoader)
                          (getResourceAsStream file))
          props (Properties.)]
      (.load props file-reader)
      (into {} props))
    (catch Exception e nil)))

(def ^:dynamic *reply-pom-properties* "META-INF/maven/reply/reply/pom.properties")

(defn get-version
  "attempts to get reply version from system properties or pom properties file, returns the empty string if neither works"
  []
  (or (System/getProperty "reply.version")
      (-> *reply-pom-properties*
          map-from-property-filepath
          (get "version"))
      ""))