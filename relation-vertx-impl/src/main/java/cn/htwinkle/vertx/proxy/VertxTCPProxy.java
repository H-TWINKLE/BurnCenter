package cn.htwinkle.vertx.proxy;

import cn.htwinkle.vertx.proxy.constants.Const;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.streams.Pump;

public class VertxTCPProxy {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        NetServerOptions options = new NetServerOptions();
        options.setReadIdleTimeout(120); // 设置socket多长时间未收到消息超时（秒）

        NetServer server = vertx.createNetServer(options);

        server.connectHandler(clientSocket -> {
            vertx.createNetClient().connect(Const.TARGET_PORT, Const.LISTEN_HOST, targetSocket -> {
                if (targetSocket.succeeded()) {
                    NetSocket serverSocket = targetSocket.result();

                    Pump pumpToClient = Pump.pump(serverSocket, clientSocket);
                    Pump pumpToServer = Pump.pump(clientSocket, serverSocket);

                    pumpToClient.start();
                    pumpToServer.start();
                } else {
                    clientSocket.close();
                }
            });
        });

        server.listen(Const.LISTEN_PORT,Const.LISTEN_HOST, result -> {
            if (result.succeeded()) {
                System.out.println("TCP Proxy Server listening on port " + Const.LISTEN_PORT);
            } else {
                System.err.println("Failed to start TCP Proxy Server: " + result.cause().getMessage());
            }
        });
    }
}
