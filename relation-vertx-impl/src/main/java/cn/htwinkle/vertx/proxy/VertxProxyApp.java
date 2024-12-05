package cn.htwinkle.vertx.proxy;

import cn.htwinkle.vertx.proxy.verticle.Http2ServerVerticle;
import cn.htwinkle.vertx.proxy.verticle.NetClientVeticle;
import io.vertx.core.Vertx;

public class VertxProxyApp {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new Http2ServerVerticle());
        vertx.deployVerticle(new NetClientVeticle());
    }
}
