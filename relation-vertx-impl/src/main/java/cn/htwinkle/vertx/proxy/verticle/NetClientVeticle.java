package cn.htwinkle.vertx.proxy.verticle;

import cn.htwinkle.vertx.proxy.constants.Const;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetClientVeticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetClientVeticle.class);

    private NetServer tcpServer;

    @Override
    public void start() throws Exception {
        super.start();

        /*WebClientOptions webClientOptions = new WebClientOptions()
                .setUseAlpn(true)
                .setSsl(false)
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setHttp2ClearTextUpgrade(false)
                .setTrustAll(true)
                .setLogActivity(true);
        WebClient webClient = WebClient.create(vertx, webClientOptions);*/


        NetServerOptions options = new NetServerOptions()
                .setLogActivity(true);

        tcpServer = vertx.createNetServer(options);
        tcpServer.connectHandler(socket -> {
            LOGGER.info("client received new connection");

            vertx.createNetClient().connect(Const.WEB_PORT, Const.WEB_HOST, conn -> {
                if (conn.succeeded()) {
                    NetSocket serverSocket = conn.result();

                    connectToServer(serverSocket);

                    /*webClient.get(Const.WEB_PORT, Const.WEB_HOST, Const.URL)
                    .addQueryParam(Const.KEY_TARGET, Const.TARGET_HOST + ":" + Const.TARGET_PORT)
                    .putHeader(HttpHeaders.CONNECTION.toString(), HttpHeaders.UPGRADE.toString())
                    .sendStream(serverSocket)
                    .onSuccess(response -> {
                        // 将HTTP/2响应写回TCP客户端
                        LOGGER.info("response received" + response.bodyAsString());
                        LOGGER.info("Received response with status code" + response.statusCode());
                    })
                    .onFailure(err -> {
                        LOGGER.error("error : " + ExceptionUtil.stacktraceToString(err));
                    });*/
                }
            });


//            socket.handler(b -> {
//                LOGGER.info("client socket handler buffer " + b.toString());
//                webClient.get(Const.WEB_PORT, Const.WEB_HOST, Const.URL)
//                        .addQueryParam(Const.KEY_TARGET, Const.TARGET_HOST + ":" + Const.TARGET_PORT)
//                        .putHeader(HttpHeaders.CONNECTION.toString(), HttpHeaders.UPGRADE.toString())
//                        .sendStream(socket)
//                        .onSuccess(response -> {
//                            // 将HTTP/2响应写回TCP客户端
//                            LOGGER.info("response received" + response.bodyAsString());
//                            LOGGER.info("Received response with status code" + response.statusCode());
//                        })
//                        .onFailure(err -> {
//                            LOGGER.error("error : " + ExceptionUtil.stacktraceToString(err));
//                        });
//            });

            socket.drainHandler(v -> {
                LOGGER.info("client socket drained");
            });
            socket.exceptionHandler(t -> {
                LOGGER.info("client socket exception");
                t.printStackTrace();
            });
            socket.closeHandler(v -> {
                LOGGER.info("client socket closed");
            });
            socket.endHandler(v -> {
                LOGGER.info("client socket ended");
            });

        });

        tcpServer.listen(Const.LISTEN_PORT, Const.LISTEN_HOST, ar -> {
            if (ar.succeeded()) {
                LOGGER.info("tcp server started on port " + ar.result().actualPort());
            } else {
                ar.cause().printStackTrace();
            }
        });
    }

    private void connectToServerBy(NetSocket socket, Buffer buffer) {
        WebClientOptions webClientOptions = new WebClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setUseAlpn(true)
                .setHttp2ClearTextUpgrade(false)
                .setTrustAll(true);
        WebClient webClient = WebClient.create(vertx, webClientOptions);
        webClient.post(Const.WEB_PORT, Const.WEB_HOST, Const.URL)
                .addQueryParam(Const.KEY_TARGET, Const.TARGET_HOST + ":" + Const.TARGET_PORT)
                .putHeader(HttpHeaderValues.UPGRADE.toString(), HttpHeaderValues.UPGRADE.toString())
                .sendBuffer(buffer, res -> {
                    if (res.succeeded()) {
                        socket.write(res.result().bodyAsBuffer());
                    }
                });
    }

    private void connectToServer(NetSocket clientSocket) {
        String requestURI = Const.URL + "?" + Const.KEY_TARGET + "=" + Const.TARGET_HOST + ":" + Const.TARGET_PORT;
        HttpClientOptions httpClientOptions = new HttpClientOptions()
                .setUseAlpn(true)
                .setSsl(false)
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setHttp2ClearTextUpgrade(false)
                .setTrustAll(true)
                .setLogActivity(true);
        vertx.createHttpClient(httpClientOptions)
                .request(HttpMethod.GET, Const.WEB_PORT, Const.WEB_HOST, requestURI)
                .onSuccess(result -> {
                    result.putHeader(HttpHeaders.CONNECTION, HttpHeaderValues.UPGRADE)
                            .send(clientSocket, res -> {
                                HttpClientResponse httpClientResponse = res.result();
                                if (res.succeeded()) {
                                    httpClientResponse.handler(b -> {
                                        LOGGER.info("client httpClientResponse get server buffer: " + b);
                                        // 将HTTP/2响应写回TCP客户端
                                        clientSocket.write(b);
                                        clientSocket.close();
                                    });
                                }
                                if (res.failed()) {
                                    res.cause().printStackTrace();
                                }

                            });

                    /*result.response(resp -> {
                        if (resp.succeeded()) {
                            HttpClientResponse httpClientResponse = resp.result();
                            httpClientResponse.handler(b -> {
                                LOGGER.info("client httpClientResponse get server buffer: " + b);

                                // 将HTTP/2响应写回TCP客户端
                                clientSocket.write(b);
                                clientSocket.close();
                            });
                            httpClientResponse.body(b -> {
                                LOGGER.info("client httpClientResponse handler buffer " + b.toString());
                            });
                            httpClientResponse.endHandler(v -> {
                                LOGGER.info("client httpClientResponse end");
                            });
                            httpClientResponse.streamPriorityHandler(s -> {
                                LOGGER.info("client httpClientResponse streamPriority" + s.toString());
                            });
                            httpClientResponse.exceptionHandler(e -> {
                                LOGGER.info("client httpClientResponse exceptionHandler");
                                e.printStackTrace();
                            });
                            httpClientResponse.customFrameHandler(h -> {
                                Buffer payload = h.payload();
                                LOGGER.info("client httpClientResponse customFrameHandler buffer " + payload.toString());
                            });
                        }
                    });*/
                })
                .onFailure(e -> {
                    e.printStackTrace();
                });
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (tcpServer != null) {
            tcpServer.close();
        }
    }
}
