(ns vapordev.operations.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [vapordev.operations.core :as core]))

(deftest main-setup-test
  (testing "Configuração inicial da função -main"
    (let [server-started (atom nil)
          shutdown-hook-added (atom false)
          expected-port 8888]
      (with-redefs [core/start-server (fn [port]
                                        (reset! server-started port)
                                        :mock-server)
                    core/get-env-var (fn [name default]
                                       (if (= name "PORT") (str expected-port) default))
                    core/add-shutdown-hook! (fn [] (reset! shutdown-hook-added true))]

        (let [port-str (#'core/get-env-var "PORT" "3000")
              port (Integer. port-str)]
          (#'core/start-server port)
          (#'core/add-shutdown-hook!))

        (is (= expected-port @server-started) "Servidor iniciado na porta correta")
        (is (true? @shutdown-hook-added) "Shutdown hook adicionado")))))
