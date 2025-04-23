(ns vapordev.operations.handler-test
  (:require [clojure.test :refer [deftest is testing]]
            [ring.mock.request :as mock]
            [vapordev.operations.handler :refer [api-handler]]
            [vapordev.operations.service :as service]
            [cheshire.core :as json])
  (:import [java.util.concurrent Future]))

(deftest post-operations-endpoint-test
  (testing "POST /operations endpoint"
    (let [service-called (atom false)
          test-ops [{:op "buy"}]]
      (with-redefs [service/process-operations (fn [ops]
                                                 (reset! service-called true)
                                                 (is (= test-ops ops))
                                                 (reify Future (get [_] nil)))]
        (let [request (-> (mock/request :post "/operations")
                          (mock/content-type "application/json")
                          (mock/body (json/generate-string test-ops)))
              response (api-handler request)]
          (is (= 202 (:status response)))
          (is (= {:message "Operations received and processing initiated."}
                 (json/parse-string (:body response) true)))
          (is (true? @service-called)))))))

(deftest post-operations-empty-test
  (testing "POST /operations com corpo vazio"
    (let [service-called (atom false)]
      (with-redefs [service/process-operations (fn [_] (reset! service-called true))]
        (let [request (-> (mock/request :post "/operations")
                          (mock/content-type "application/json")
                          (mock/body (json/generate-string [])))
              response (api-handler request)]
          (is (= 202 (:status response)))
          (is (= {:message "No operations provided, but request accepted."}
                 (json/parse-string (:body response) true)))
          (is (false? @service-called)))))))

(deftest post-operations-malformed-test
  (testing "POST /operations com JSON mal formado"
    (let [request (-> (mock/request :post "/operations")
                      (mock/content-type "application/json")
                      (mock/body "{\"op\":}"))
          response (api-handler request)]
      (is (= 400 (:status response)))
      (is (= {:error "Malformed JSON request body."}
             (json/parse-string (:body response) true))))))

(deftest get-health-endpoint-test
  (testing "GET /health endpoint"
    (let [mock-health {:app "mock-app", :status "mock-status", :kafka "mock-kafka"}]
      (with-redefs [service/get-health-status (fn [] mock-health)]
        (let [request (mock/request :get "/health")
              response (api-handler request)
              body (json/parse-string (:body response) true)]
          (is (= 200 (:status response)))
          (is (= mock-health body)))))))

(deftest not-found-route-test
  (testing "Rota não encontrada"
    (let [request (mock/request :get "/nonexistent")
          response (api-handler request)]
      (is (= 404 (:status response)))
      (is (= {:error "Not Found"} (json/parse-string (:body response) true))))))
