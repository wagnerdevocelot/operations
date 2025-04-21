(ns vapordev.operations
  (:require
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.util.response :refer [response content-type]]
   [cheshire.core :as json]
   [jackdaw.client :as kafka]
   [jackdaw.serdes :refer [string-serde edn-serde]])
  (:import [java.util UUID]
           [java.util.concurrent Executors])
  (:gen-class))

(def topic-config
  "Configuração do tópico Kafka"
  {:topic-name "operations"
   :partition-count 1
   :replication-factor 1
   :key-serde (string-serde)
   :value-serde (edn-serde)})

(def producer-config
  "Configuração do produtor Kafka"
  {"bootstrap.servers" "localhost:9092"
   "key.serializer" "org.apache.kafka.common.serialization.StringSerializer"
   "value.serializer" "org.apache.kafka.common.serialization.StringSerializer"})

(defn create-producer
  "Cria uma instância do produtor Kafka"
  []
  (kafka/producer producer-config))

;; Criar um pool de threads para envios assíncronos ao Kafka
(def executor (Executors/newFixedThreadPool 4))

(defn send-to-kafka-async
  "Envia operações para o Kafka de forma assíncrona"
  [operations]
  (.submit executor
           (reify java.util.concurrent.Callable
             (call [_]
               (try
                 (with-open [producer (create-producer)]
                   (doseq [operation operations]
                     (kafka/produce! producer topic-config (str (UUID/randomUUID)) operation)))
                 (println "Operações enviadas com sucesso para o Kafka")
                 (catch Exception e
                   (println "Erro ao enviar operações para o Kafka:" (.getMessage e))))))))

(defn handler
  "Handler para o endpoint /operations que processa operações recebidas.
   Envia as operações para o tópico Kafka de forma assíncrona."
  [request]
  (try
    (let [operations (json/parse-string (slurp (:body request)) true)]
      (try
        ;; Enviar para o Kafka assincronamente
        (send-to-kafka-async operations)

        ;; Retornar resposta imediatamente
        (-> (json/generate-string {:message "Operations processed successfully"})
            response
            (content-type "application/json")
            (assoc :status 201))
        (catch Exception e
          (println "Erro ao preparar envio para o Kafka:" (.getMessage e))
          (-> (json/generate-string {:error (str "Erro ao preparar envio para o Kafka: " (.getMessage e))})
              response
              (content-type "application/json")
              (assoc :status 500)))))
    (catch Exception e
      (println "Erro ao processar requisição:" (.getMessage e))
      (-> (json/generate-string {:error (.getMessage e)})
          response
          (content-type "application/json")
          (assoc :status 500)))))

(defn start-server
  "Inicia o servidor HTTP na porta especificada."
  [port]
  (run-jetty handler {:port port :join? false}))

(defn get-env-var
  "Função wrapper para obter variáveis de ambiente"
  [name]
  (System/getenv name))

(defn -main
  "Ponto de entrada principal da aplicação.
   Inicia o servidor na porta definida pela variável de ambiente PORT ou 3000 por padrão."
  [& _]
  (let [port (Integer. (or (get-env-var "PORT") 3000))]
    (println (str "Starting server on port " port))
    (start-server port)))
