package com.portfolio.mapapp;

import com.portfolio.mapapp.config.ObjectMapperProvider;
import com.portfolio.mapapp.filter.CorsFilter;
import com.portfolio.mapapp.resource.CategoryResource;
import com.portfolio.mapapp.resource.LocationResource;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;

/**
 * 組み込み Grizzly HTTP サーバーで Jersey アプリを起動するエントリポイント。
 *
 * <p>{@code java -jar target/map-app.jar} または {@code mvn exec:java} で単体起動できる。
 * ポートは環境変数 PORT で変更可能(既定: 8080)。</p>
 */
public class Main {

    public static void main(String[] args) throws IOException {
        String port = System.getenv().getOrDefault("PORT", "8080");
        URI baseUri = URI.create("http://0.0.0.0:" + port + "/api/");

        // 使用するリソースとプロバイダを登録
        ResourceConfig config = new ResourceConfig()
                .register(LocationResource.class)
                .register(CategoryResource.class)
                .register(JacksonFeature.class)          // JSON連携を有効化
                .register(ObjectMapperProvider.class)    // ObjectMapperのカスタム設定
                .register(CorsFilter.class);             // CORS（開発用: localhost:5173を許可）

        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, config);

        // Ctrl+C 等での終了時にサーバーを安全に停止
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nサーバーを停止します...");
            server.shutdownNow();
        }));

        System.out.println("=================================================");
        System.out.println(" map-app サーバーを起動しました");
        System.out.println(" ベースURL : " + baseUri);
        System.out.println(" 例: GET  " + baseUri + "locations");
        System.out.println("     POST " + baseUri + "locations");
        System.out.println(" 停止するには Ctrl+C");
        System.out.println("=================================================");

        // メインスレッドを生かしたままにする
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
