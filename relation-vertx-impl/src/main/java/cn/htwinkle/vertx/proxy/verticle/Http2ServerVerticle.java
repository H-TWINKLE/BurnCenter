package cn.htwinkle.vertx.proxy.verticle;

import cn.htwinkle.vertx.proxy.constants.Const;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Http2ServerVerticle extends AbstractVerticle {
    private HttpServer httpServer;

    private static final Logger LOGGER = LoggerFactory.getLogger(Http2ServerVerticle.class);

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);
        router.post(Const.URL).handler(ctx -> {

            HttpServerRequest request = ctx.request();
            String param = request.getParam(Const.KEY_TARGET);
            String targetHost = param.split(":")[0];
            int targetPort = Integer.parseInt(param.split(":")[1]);

            request.toNetSocket(clientNetSocket -> {
                if (clientNetSocket.succeeded()) {
                    NetSocket clientSocket = clientNetSocket.result();

                    clientSocket.closeHandler(h -> {
                        LOGGER.info("server clientSocket closed");
                    });
                    clientSocket.endHandler(h -> {
                        LOGGER.info("server clientSocket end");
                    });
                    clientSocket.drainHandler(h -> {
                        LOGGER.info("server clientSocket drain");
                    });
                    clientSocket.exceptionHandler(h -> {
                        LOGGER.info("server clientSocket exception");
                        h.printStackTrace();
                    });
                    clientSocket.handler(b -> {
                        LOGGER.info("server clientSocket buffer: " + b.toString());
                        clientSocket.pause();

                        vertx.createNetClient().connect(targetPort, targetHost, targetSocket -> {
                            if (targetSocket.succeeded()) {
                                NetSocket serverSocket = targetSocket.result();

                                serverSocket.closeHandler(h -> {
                                    LOGGER.info("server serverSocket closed");
                                });
                                serverSocket.endHandler(h -> {
                                    LOGGER.info("server serverSocket end");
                                });
                                serverSocket.drainHandler(h -> {
                                    LOGGER.info("server serverSocket drain");
                                });
                                serverSocket.exceptionHandler(h -> {
                                    LOGGER.info("server serverSocket exception");
                                    h.printStackTrace();
                                });
                                serverSocket.handler(buffer -> {
                                    LOGGER.info("server serverSocket get buffer: " + buffer.toString());

                                    //clientSocket.resume();

                                    clientSocket.write(buffer);

                                    /*Pump pumpToClient = Pump.pump(serverSocket, clientSocket);
                                    Pump pumpToServer = Pump.pump(clientSocket, serverSocket);

                                    pumpToClient.start();
                                    pumpToServer.start();*/
                                });

                                // request.response().writeCustomFrame(new HttpFrameImpl(Constants.frameType, Constants.frameStatus,));
                            }
                            if (targetSocket.failed()) {
                                targetSocket.cause().printStackTrace();
                            }
                        });
                    });
                }
                if (clientNetSocket.failed()) {
                    clientNetSocket.cause().printStackTrace();
                }
            });

        });

        HttpServerOptions options = new HttpServerOptions()
                .setHttp2ClearTextEnabled(true)
                .setUseAlpn(true);

        httpServer = vertx.createHttpServer(options).requestHandler(router);
        httpServer.listen(Const.WEB_PORT, Const.WEB_HOST, ar -> {
            if (ar.succeeded()) {
                LOGGER.info("http server started on port " + ar.result().actualPort());
            } else {
                ar.cause().printStackTrace();
            }
        });
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (httpServer != null) {
            httpServer.close();
        }
    }
}
