(ns vapordev.operations.service
  (:require [vapordev.operations.kafka :as kafka]))

(defn process-operations
  "Processa uma lista de operações, enviando-as para o Kafka.
   Retorna o Future retornado pela camada Kafka."
  [operations]
  (kafka/produce-operations-event! operations))

(defn get-health-status
  "Obtém o status de saúde da aplicação, incluindo a conexão Kafka."
  []
  {:app   "operations-service"
   :status "up"
   :kafka (kafka/check-connection)})
