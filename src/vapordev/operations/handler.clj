(ns vapordev.operations.handler
  (:require [ring.util.response :refer [response content-type status]]
            [cheshire.core :as json]
            [vapordev.operations.service :as service]))

(defn ^:private json-response
  "Cria uma resposta Ring com corpo JSON."
  [data & {:keys [status-code] :or {status-code 200}}]
  (-> (json/generate-string data)
      response
      (content-type "application/json")
      (status status-code)))

(defn ^:private handle-operations-post
  "Handler para POST /operations. Processa operações enviadas no corpo da requisição."
  [request]
  (try
    (let [operations (json/parse-string (slurp (:body request)) true)]
      (if (empty? operations)
        (json-response {:message "No operations provided, but request accepted."} :status-code 202)
        (do
          (service/process-operations operations)
          (json-response {:message "Operations received and processing initiated."} :status-code 202))))
    (catch com.fasterxml.jackson.core.JsonParseException e
      (println "Erro ao parsear JSON da requisição:" (.getMessage e))
      (json-response {:error "Malformed JSON request body."} :status-code 400))
    (catch Exception e
      (println "Erro interno ao processar /operations:" (.getMessage e))
      (json-response {:error (str "Internal server error: " (.getMessage e))} :status-code 500))))

(defn ^:private handle-health-get
  "Handler para GET /health. Retorna o status de saúde do serviço."
  [_request]
  (try
    (json-response (service/get-health-status))
    (catch Exception e
      (println "Erro ao verificar health status:" (.getMessage e))
      (json-response {:app "operations-service" :status "error" :details (.getMessage e)} :status-code 503))))

(defn api-handler
  "Handler principal da API que roteia as requisições. Define rotas para /operations e /health."
  [request]
  (case [(:request-method request) (:uri request)]
    [:post "/operations"] (handle-operations-post request)
    [:get  "/health"]     (handle-health-get request)
    (json-response {:error "Not Found"} :status-code 404)))