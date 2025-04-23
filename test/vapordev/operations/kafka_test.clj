(ns vapordev.operations.kafka-test
  (:require [clojure.test :refer [deftest is testing]]
            [vapordev.operations.kafka :as kafka]
            [jackdaw.client :as jackdaw-client]
            [cheshire.core :as json])
  (:import [clojure.lang IDeref]
           [java.io Closeable]
           [java.util.concurrent Future Executors TimeUnit]))

(deftype MockProducer []
  IDeref (deref [_] nil)
  Closeable (close [_] nil))

(deftest produce-operations-event-test
  (testing "Envio assíncrono para o Kafka via camada Kafka"
    (let [captured-data (atom nil)
          operations [{:operation "buy", :unit-cost 10.00, :quantity 1000}
                      {:operation "sell", :unit-cost 20.00, :quantity 5000}]]
      (with-redefs [kafka/create-producer (fn [] (MockProducer.))
                    jackdaw-client/produce! (fn [_ _ _ data]
                                              (reset! captured-data data)
                                              nil)
                    kafka/executor (Executors/newSingleThreadExecutor)]
        (let [future-result (kafka/produce-operations-event! operations)]
          (is (instance? Future future-result))
          (.get future-result 100 TimeUnit/MILLISECONDS)
          (is (= (json/parse-string @captured-data true) operations))
          (.shutdownNow kafka/executor))))))

(deftest check-connection-test
  (testing "Verificação de conexão Kafka"
    (testing "Conexão bem-sucedida"
      (with-redefs [kafka/create-producer (fn [] (MockProducer.))
                    kafka/get-kafka-bootstrap-servers (fn [] "mock-ok:9092")]
        (let [status (kafka/check-connection)]
          (is (= "connected" (:status status)))
          (is (= "mock-ok:9092" (:bootstrap-servers status)))
          (is (= "operations" (:topic status))))))
    (testing "Falha na conexão"
      (with-redefs [kafka/create-producer (fn [] (throw (Exception. "Connection failed")))
                    kafka/get-kafka-bootstrap-servers (fn [] "mock-fail:9092")]
        (let [status (kafka/check-connection)]
          (is (= "disconnected" (:status status)))
          (is (= "Connection failed" (:error status)))
          (is (= "mock-fail:9092" (:bootstrap-servers status))))))))
