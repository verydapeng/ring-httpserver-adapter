(ns ring.adapter.httpserver
  (:import [com.sun.net.httpserver HttpServer HttpHandler HttpExchange]
           [java.net InetSocketAddress]))

(defn- build-request-map [^HttpExchange he]
  (let [uri (.getRequestURI he)]
    {:uri (.getPath uri)
     :request-method :get}))


(defn- send-response [m ^HttpExchange he]
  (let [{:keys [status] :or {status 200}} m]
    (prn status)
    (.sendResponseHeaders he status 0)
    (.. he (getResponseBody) (write (.getBytes "hello")))))


(defn- invoke [h ^HttpExchange he]
  (let [request-map (build-request-map he)
        response-map (h request-map)]
    (send-response (or response-map {:status 404}) he)
    (.close he)))


(defn- adapt-handler [h]
  "convet the ring handler into a HttpHandler"
  (reify HttpHandler
    (^void handle [_ ^HttpExchange he]
           (invoke h he))))


(defn start [h]
  (let [server (HttpServer/create)
        ctx (.createContext server  "/")]
    (.setHandler ctx (adapt-handler h))
    (.bind server (InetSocketAddress. 9876) 128)
    (.start server)
    (fn []
      (.stop server 0))))



(def server (start (fn [request] (do
                                   (prn "doing my work")
                                   {:status 200}))))

(server)


(slurp "http://localhost:9876")
