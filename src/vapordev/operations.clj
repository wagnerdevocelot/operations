(ns vapordev.operations
  (:require
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.util.response :refer [response content-type]]
   [cheshire.core :as json]
   [jackdaw.client :as kafka]
   [jackdaw.serdes :refer [string-serde]])
  (:import [java.util UUID]
           [java.util.concurrent Executors])
  (:gen-class))

(def topic-config
  "Configuração do tópico Kafka"
  {:topic-name "operations"
   :partition-count 1
   :replication-factor 1
   :key-serde (string-serde)
   :value-serde (string-serde)})

(defn get-kafka-bootstrap-servers
  "Obtém os servidores bootstrap do Kafka da variável de ambiente ou usa o padrão"
  []
  (or (System/getenv "KAFKA_BOOTSTRAP_SERVERS") "localhost:9092"))

(def producer-config
  "Configuração do produtor Kafka"
  {"bootstrap.servers" (get-kafka-bootstrap-servers)
   "key.serializer" "org.apache.kafka.common.serialization.StringSerializer"
   "value.serializer" "org.apache.kafka.common.serialization.StringSerializer"})

(defn create-producer
  "Cria uma instância do produtor Kafka"
  []
  (kafka/producer producer-config))

;; Criar um pool de threads para envios assíncronos ao Kafka
(def executor (Executors/newFixedThreadPool 4))

(defn send-to-kafka-async
  "Envia todas as operações para o Kafka como um único evento de forma assíncrona"
  [operations]
  (.submit executor
           (reify java.util.concurrent.Callable
             (call [_]
               (try
                 (with-open [producer (create-producer)]
                   ;; Enviando todas as operações como um único evento
                   (let [event-id (str (UUID/randomUUID))]
                     (kafka/produce! producer 
                                    topic-config 
                                    event-id 
                                    (json/generate-string operations))))
                 (println "Operações enviadas com sucesso para o Kafka como um evento único")
                 (catch Exception e
                   (println "Erro ao enviar operações para o Kafka:" (.getMessage e))))))))

(defn kafka-health-check
  "Verifica a conectividade com o Kafka"
  []
  (try
    (with-open [producer (create-producer)]
      {:status "connected"
       :bootstrap-servers (get-kafka-bootstrap-servers)
       :topic (get topic-config :topic-name)})
    (catch Exception e
      {:status "disconnected"
       :error (.getMessage e)
       :bootstrap-servers (get-kafka-bootstrap-servers)})))

(defn handler
  "Handler para o endpoint /operations que processa operações recebidas.
   Envia as operações para o tópico Kafka de forma assíncrona."
  [request]
  (case [(:request-method request) (:uri request)]
    [:post "/operations"]
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
            (assoc :status 500))))
    
    [:get "/health"]
    (-> (json/generate-string {:app "operations-service"
                                :status "up"
                                :kafka (kafka-health-check)})
        response
        (content-type "application/json"))
    
    ;; Rota não encontrada
    (-> (json/generate-string {:error "Not found"})
        response
        (content-type "application/json")
        (assoc :status 404))))

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
