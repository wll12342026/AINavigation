package com.campus.freshman.rag;
import org.junit.jupiter.api.Test;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ModelGatewayTest {
    @Test void invokesConfiguredChatEndpointAndUsesReturnedUsage() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);AtomicReference<String> request=new AtomicReference<>();
        server.createContext("/v1/chat/completions", exchange -> {
            request.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            byte[] bytes="{\"choices\":[{\"message\":{\"content\":\"这是测试服务返回的文本\"}}],\"usage\":{\"prompt_tokens\":11,\"completion_tokens\":7}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });server.start();
        try {
            var value=new ModelConnection();value.kind="CHAT";value.baseUrl="http://127.0.0.1:"+server.getAddress().getPort()+"/v1";value.modelId="fixture-chat";
            var generated=new ModelGateway(mock(ModelConnectionRepository.class)).generate(value,"系统提示词","用户问题");
            assertThat(generated.text()).isEqualTo("这是测试服务返回的文本");assertThat(generated.inputTokens()).isEqualTo(11);assertThat(generated.outputTokens()).isEqualTo(7);assertThat(request.get()).contains("fixture-chat","系统提示词","用户问题");
        } finally {server.stop(0);}
    }
}
