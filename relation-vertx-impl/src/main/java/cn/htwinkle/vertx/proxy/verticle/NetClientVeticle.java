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

        NetServerOptions options = new NetServerOptions();

        String requestURI = Const.URL + "?" + Const.KEY_TARGET + "=" + Const.TARGET_HOST + ":" + Const.TARGET_PORT;

        tcpServer = vertx.createNetServer(options);
        tcpServer.connectHandler(socket -> {
            Buffer buffer = Buffer.buffer();
            socket.write(buffer);

            LOGGER.info("client get client buffer: " + buffer);
            connectToServer(socket, requestURI, buffer);

            socket.handler(b -> {
                LOGGER.info("client socket handler buffer " + buffer.toString());
            });
            socket.drainHandler(d -> {
                LOGGER.info("client socket drained");
            });
            socket.exceptionHandler(t -> {
                LOGGER.info("client socket exception");
                t.printStackTrace();
            });
            socket.closeHandler(h -> {
                LOGGER.info("client socket closed");
            });
            socket.endHandler(e -> {
                LOGGER.info("client socket ended");
            });

        });

        tcpServer.listen(Const.LISTEN_PORT, Const.LISTEN_HOST, ar ->

        {
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

    private void connectToServer(NetSocket socket, String requestURI, Buffer buffer) {
        HttpClientOptions httpClientOptions = new HttpClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setUseAlpn(true)
                .setHttp2ClearTextUpgrade(true)
                .setTrustAll(true);
        HttpClient httpClient = vertx.createHttpClient(httpClientOptions);
        httpClient.request(HttpMethod.POST, Const.WEB_PORT, Const.WEB_HOST, requestURI)
                .onSuccess(result -> {
                    result.putHeader(HttpHeaderValues.UPGRADE.toString(), HttpHeaderValues.UPGRADE.toString())
                            .end(buffer);
                    result.response(resp -> {
                        if (resp.succeeded()) {
                            HttpClientResponse httpClientResponse = resp.result();
                            httpClientResponse.handler(b -> {
                                LOGGER.info("client httpClientResponse get server buffer: " + b);
                                socket.write(b);
                                //socket.close();
                            });
                            httpClientResponse.body(b -> {
                                LOGGER.info("client httpClientResponse handler buffer " + b.toString());
                            });
                            httpClientResponse.endHandler(b -> {
                                LOGGER.info("client httpClientResponse end");
                            });
                            httpClientResponse.streamPriorityHandler(b -> {
                                LOGGER.info("client httpClientResponse streamPriority" + b.toString());
                            });
                            httpClientResponse.exceptionHandler(b -> {
                                LOGGER.info("client httpClientResponse exceptionHandler");
                                b.printStackTrace();
                            });
                            httpClientResponse.customFrameHandler(h -> {
                                Buffer payload = h.payload();
                                LOGGER.info("client httpClientResponse customFrameHandler buffer " + payload.toString());
                            });
                        }
                    });
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
