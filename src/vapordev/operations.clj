(ns vapordev.operations
  (:require
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.util.response :refer [response]]
   [cheshire.core :as json]
   [jackdaw.client :as kafka]
   [jackdaw.serdes :refer [string-serde edn-serde]])
  (:import [java.util UUID])
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

(defn handler
  "Handler para o endpoint /operations que processa operações recebidas.
   Envia as operações para o tópico Kafka."
  [request]
  (let [operations (json/parse-string (slurp (:body request)) true)]
    (with-open [producer (create-producer)]
      (doseq [operation operations]
        (kafka/produce! producer topic-config (str (UUID/randomUUID)) operation)))
    (response {:message "Operations processed successfully"})))

(defn start-server
  "Inicia o servidor HTTP na porta especificada."
  [port]
  (run-jetty handler {:port port}))

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
