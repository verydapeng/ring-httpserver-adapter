(ns ring.adapter.httpserver
  (:require [clojure.java.io :as io])
  (:import [com.sun.net.httpserver HttpServer HttpHandler HttpExchange]
           [java.io File OutputStream InputStream FileInputStream]
           [java.nio.charset StandardCharsets]
           [java.net InetSocketAddress]))

(def request-methods {"GET"  :get
                      "POST" :post
                      "PUT"  :put})


(defn- flatten-header-entry [entry]
  (let [[k v] entry
        v (if (= 1 (count v)) (first v) v)]
    [k v]))


(defn- build-request-map [^HttpExchange he]
  (let [uri (.getRequestURI he)]
    {:uri (.getPath uri)
     :query-string (.getRawQuery uri)
     :request-method (get request-methods (.getRequestMethod he) :get)
     :headers (into {} (map flatten-header-entry (.. he (getRequestHeaders))))
     :scheme :http
     :remote-addr (.. he (getRemoteAddress) (getHostString))
     :body (.getRequestBody he)}))


(defn- set-body
  [^OutputStream out, body]
  (cond
   (string? body)
   (.write out (.getBytes body StandardCharsets/UTF_8))

   (seq? body)
   (doseq [chunk body]
     (.write out (.getBytes (str chunk) StandardCharsets/UTF_8)))

   (instance? InputStream body)
   (with-open [^InputStream b body]
     (io/copy b out))

   (instance? File body)
   (with-open [b (FileInputStream. body)]
     (set-body out b))

   (nil? body)
   nil

   :else
   (throw (Exception. ^String (format "Unrecognized body: %s" body)))))


(defn- send-response [m ^HttpExchange he]
  (let [{:keys [status headers body] :or {status 200 headers {} body nil}} m]
    (->> headers
         (map #(let [[k v] %] (.. he (getResponseHeaders) (add k v))))
         doall)
    (.sendResponseHeaders he status 0)
    (set-body (.getResponseBody he) body)))


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
