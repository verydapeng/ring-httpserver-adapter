(in-ns ring.adapter.httpserver)

(def stop-server (start (fn [request] (do
                                   (prn request)
                                   {:status 200
                                    :body (File. "/home/dapeng/Temp/aa.clj")
                                    :headers {"a" "b"}}))))

(stop-server)

(slurp "http://localhost:9876")
