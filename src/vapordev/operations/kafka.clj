(ns vapordev.operations.kafka
  (:require [jackdaw.client :as kafka]
            [jackdaw.serdes :refer [string-serde]]
            [cheshire.core :as json])
  (:import [java.util UUID]
           [java.util.concurrent Executors Future]))

(def ^:private topic-config
  "Configuração do tópico Kafka"
  {:topic-name         "operations"
   :partition-count    1
   :replication-factor 1
   :key-serde          (string-serde)
   :value-serde        (string-serde)})

(defn ^:private get-kafka-bootstrap-servers
  "Obtém os servidores bootstrap do Kafka da variável de ambiente ou usa o padrão"
  []
  (or (System/getenv "KAFKA_BOOTSTRAP_SERVERS") "localhost:9092"))

(def ^:private producer-config
  "Configuração do produtor Kafka"
  {"bootstrap.servers"  (get-kafka-bootstrap-servers)
   "key.serializer"     "org.apache.kafka.common.serialization.StringSerializer"
   "value.serializer"   "org.apache.kafka.common.serialization.StringSerializer"})

(defn ^:private create-producer
  "Cria uma instância do produtor Kafka"
  []
  (kafka/producer producer-config))

(def executor (Executors/newFixedThreadPool 4))

(defn produce-operations-event!
  "Envia um evento contendo operações para o Kafka de forma assíncrona usando um pool de threads.
   Retorna um Future."
  ^Future [operations]
  (.submit executor
           (reify java.util.concurrent.Callable
             (call [_]
               (try
                 (with-open [producer (create-producer)]
                   (let [event-id (str (UUID/randomUUID))]
                     (kafka/produce! producer
                                     topic-config
                                     event-id
                                     (json/generate-string operations)))
                   (println "Evento de operações enviado com sucesso para o Kafka."))
                 (catch Exception e
                   (println "Erro ao enviar evento para o Kafka:" (.getMessage e))
                   (throw e)))))))

(defn check-connection
  "Verifica a conectividade com o Kafka"
  []
  (try
    (with-open [_ (create-producer)]
      {:status            "connected"
       :bootstrap-servers (get-kafka-bootstrap-servers)
       :topic             (:topic-name topic-config)})
    (catch Exception e
      {:status            "disconnected"
       :error             (.getMessage e)
       :bootstrap-servers (get-kafka-bootstrap-servers)})))

(defn shutdown-executor!
  "Encerra o pool de threads do executor."
  []
  (.shutdown executor))
