(ns vapordev.operations-test
  (:require [clojure.test :refer [deftest is testing]]
            [ring.mock.request :as mock]
            [vapordev.operations :refer [handler create-producer start-server -main get-env-var send-to-kafka-async]]
            [cheshire.core :as json]
            [jackdaw.client :as jackdaw-client]
            [ring.adapter.jetty :refer [run-jetty]])
  (:import [clojure.lang IDeref]
           [java.io Closeable]
           [java.util.concurrent Future]))

(deftype
  ^{:doc "Mock do produtor Kafka para testes"}
  MockProducer []
  IDeref
  (deref [_] nil)
  Closeable
  (close [_] nil))

(defn mock-produce!
  "Mock da função produce! do Kafka que simula o envio de mensagens"
  [_ _ _ _]
  nil)

(deftest send-to-kafka-async-test
  (testing "Envio assíncrono para o Kafka como um único evento"
    (let [captured-data (atom nil)
          operations [{"operation" "buy", "unit-cost" 10.00, "quantity" 1000}
                      {"operation" "sell", "unit-cost" 20.00, "quantity" 5000}]]
      (with-redefs [vapordev.operations/create-producer (fn [] (MockProducer.))
                    jackdaw-client/produce! (fn [_ _ _ data]
                                              (reset! captured-data data)
                                              nil)]
        (let [result (send-to-kafka-async operations)]
          ;; Verificando se retorna um Future
          (is (instance? Future result))
          
          ;; Aguardar a conclusão do Future (simplificado para testes)
          (Thread/sleep 100)
          
          ;; Verificando se os dados capturados contêm todas as operações como um único evento
          (is (= (json/parse-string @captured-data) operations)))))))

(deftest post-operations-endpoint-test
  (testing "POST /operations endpoint"
    (with-redefs [vapordev.operations/send-to-kafka-async (fn [_] (reify Future
                                                                   (isDone [_] true)
                                                                   (get [_] nil)))
                  jackdaw-client/produce! mock-produce!]
      (let [request (-> (mock/request :post "/operations")
                        (mock/content-type "application/json")
                        (mock/body (json/generate-string
                                    [{"operation" "buy"
                                      "unit-cost" 10.00
                                      "quantity" 10000}
                                     {"operation" "sell"
                                      "unit-cost" 20.00
                                      "quantity" 5000}])))
            response (handler request)]
        (is (= (:status response) 201))
        (is (= (json/parse-string (:body response) true) {:message "Operations processed successfully"}))))))

(deftest create-producer-test
  (testing "Criação do produtor Kafka"
    (with-redefs [jackdaw-client/producer (fn [_] (MockProducer.))]
      (let [producer (create-producer)]
        (is (instance? MockProducer producer))))))

(deftest handler-with-empty-request-test
  (testing "Handler com requisição vazia"
    (with-redefs [vapordev.operations/send-to-kafka-async (fn [_] (reify Future
                                                                   (isDone [_] true)
                                                                   (get [_] nil)))]
      (let [request (-> (mock/request :post "/operations")
                        (mock/content-type "application/json")
                        (mock/body (json/generate-string [])))
            response (handler request)]
        (is (= (:status response) 201))
        (is (= (json/parse-string (:body response) true) {:message "Operations processed successfully"}))))))

(deftest handler-with-malformed-request-test
  (testing "Handler com requisição mal formatada"
    (with-redefs [vapordev.operations/create-producer (fn [] (MockProducer.))
                  jackdaw-client/produce! mock-produce!]
      (let [request (-> (mock/request :post "/operations")
                        (mock/content-type "application/json")
                        (mock/body "not json"))]
        (is (= 500 (:status (handler request))))))))

(deftest start-server-test
  (testing "Inicialização do servidor HTTP"
    (with-redefs [run-jetty (fn [_ {:keys [port]}]
                              (is (= port 3000))
                              "mock-server")]
      (is (= (start-server 3000) "mock-server")))))

(deftest main-function-test
  (testing "Função main com porta padrão"
    (with-redefs [start-server (fn [port]
                                (is (= port 3000))
                                "mock-server")
                  get-env-var (fn [_] nil)]
      (is (= (-main) "mock-server"))))

  (testing "Função main com porta personalizada"
    (with-redefs [start-server (fn [port]
                                (is (= port 8080))
                                "mock-server")
                  get-env-var (fn [env-var]
                                 (when (= env-var "PORT") "8080"))]
      (is (= (-main) "mock-server")))))

