(ns vapordev.operations.main
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [vapordev.operations.handler :as handler]
            [vapordev.operations.kafka :as kafka])
  (:gen-class))

(defn ^:private get-env-var
  "Função wrapper para obter variáveis de ambiente"
  [name default-value]
  (or (System/getenv name) default-value))

(defn ^:private start-server
  "Inicia o servidor HTTP na porta especificada."
  [port]
  (println (str "Starting server on port " port))
  (run-jetty #'handler/api-handler {:port port :join? false}))

(defn ^:private add-shutdown-hook!
  "Adiciona um shutdown hook para limpar recursos."
  []
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. (fn []
                               (println "Shutting down Kafka executor...")
                               (kafka/shutdown-executor!)
                               (println "Shutdown complete.")
                               ))))

(defn -main
  "Ponto de entrada principal da aplicação."
  [& _]
  (let [port (Integer. (get-env-var "PORT" "3000"))
        _    (start-server port)]
    (add-shutdown-hook!)
    (println "Server started. Press Ctrl+C to stop.")
    (while true (Thread/sleep Long/MAX_VALUE))))
