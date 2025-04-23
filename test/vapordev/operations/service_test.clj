(ns vapordev.operations.service-test
  (:require [clojure.test :refer [deftest is testing]]
            [vapordev.operations.service :as service]
            [vapordev.operations.kafka :as kafka])
  (:import [java.util.concurrent Future]))

(deftest process-operations-test
  (testing "Processamento de operações na camada de serviço"
    (let [operations-data [{:op "test"}]
          kafka-called (atom false)]
      (with-redefs [kafka/produce-operations-event! (fn [ops]
                                                      (reset! kafka-called true)
                                                      (is (= operations-data ops))
                                                      (reify Future (get [_] :ok)))]
        (let [result (service/process-operations operations-data)]
          (is (instance? Future result))
          (is (= :ok (.get result)))
          (is (true? @kafka-called)))))))

(deftest get-health-status-test
  (testing "Obtenção do status de saúde na camada de serviço"
    (let [mock-kafka-status {:status "mocked"}]
      (with-redefs [kafka/check-connection (fn [] mock-kafka-status)]
        (let [health (service/get-health-status)]
          (is (= "operations-service" (:app health)))
          (is (= "up" (:status health)))
          (is (= mock-kafka-status (:kafka health))))))))
