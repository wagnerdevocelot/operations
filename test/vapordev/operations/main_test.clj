(ns vapordev.operations.main-test
  (:require [clojure.test :refer [deftest is testing]]
            [vapordev.operations.main :as main]))

(deftest main-setup-test
  (testing "Configuração inicial da função -main"
    (let [server-started (atom nil)
          shutdown-hook-added (atom false)
          expected-port 8888]
      (with-redefs [main/start-server (fn [port]
                                        (reset! server-started port)
                                        :mock-server)
                    main/get-env-var (fn [name default]
                                       (if (= name "PORT") (str expected-port) default))
                    main/add-shutdown-hook! (fn [] (reset! shutdown-hook-added true))]

        (let [port-str (#'main/get-env-var "PORT" "3000")
              port (Integer. port-str)]
          (#'main/start-server port)
          (#'main/add-shutdown-hook!))

        (is (= expected-port @server-started) "Servidor iniciado na porta correta")
        (is (true? @shutdown-hook-added) "Shutdown hook adicionado")))))
