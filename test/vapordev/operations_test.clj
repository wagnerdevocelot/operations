(ns vapordev.operations-test
  (:require [clojure.test :refer [deftest is testing]]
            [ring.mock.request :as mock]
            [vapordev.operations :refer [handler create-producer start-server -main get-env-var]]
            [cheshire.core :as json]
            [jackdaw.client :as jackdaw-client]
            [ring.adapter.jetty :refer [run-jetty]])
  (:import [clojure.lang IDeref]
           [java.io Closeable]))

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

(deftest post-operations-endpoint-test
  (testing "POST /operations endpoint"
    (with-redefs [vapordev.operations/create-producer (fn [] (MockProducer.))
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
        (is (= (:status response) 200))
        (is (= (:body response) {:message "Operations processed successfully"}))))))

(deftest create-producer-test
  (testing "Criação do produtor Kafka"
    (with-redefs [jackdaw-client/producer (fn [_] (MockProducer.))]
      (let [producer (create-producer)]
        (is (instance? MockProducer producer))))))

(deftest handler-with-empty-request-test
  (testing "Handler com requisição vazia"
    (with-redefs [vapordev.operations/create-producer (fn [] (MockProducer.))
                  jackdaw-client/produce! mock-produce!]
      (let [request (-> (mock/request :post "/operations")
                        (mock/content-type "application/json")
                        (mock/body (json/generate-string [])))
            response (handler request)]
        (is (= (:status response) 200))
        (is (= (:body response) {:message "Operations processed successfully"}))))))

(deftest handler-with-malformed-request-test
  (testing "Handler com requisição mal formatada"
    (with-redefs [vapordev.operations/create-producer (fn [] (MockProducer.))
                  jackdaw-client/produce! mock-produce!
                  slurp (fn [_] "not json")]
      (is (thrown? Exception (handler (mock/request :post "/operations")))))))

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

