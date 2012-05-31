(ns reply.version
  (:import java.util.Properties))

(defn load-pom-properties 
  "this loads a config file from the classpath"
  []
  (try 
    (let [file-reader (.. (Thread/currentThread)
                          (getContextClassLoader)
                          (getResourceAsStream "META-INF/maven/reply/reply/pom.properties"))
          props (Properties.)]
      (.load props file-reader)
      (into {} props))
    (catch Exception e nil)))

(defn get-version []
  (or (System/getenv "reply.version")
      (get (load-pom-properties) "version")
      "version unknown"))