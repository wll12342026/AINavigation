package com.campus.freshman.rag;

import org.springframework.stereotype.Service;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.net.*;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;

@Service
public class ModelGateway {
    private final ModelConnectionRepository repository;
    public ModelGateway(ModelConnectionRepository repository) {this.repository=repository;}
    public ConnectionView view(ModelConnection value) {
        return new ConnectionView(value.getId(),value.name,value.kind,value.baseUrl,value.modelId,value.apiKeyEnv,
                value.apiKeyEnv==null || value.apiKeyEnv.isBlank() || System.getenv(value.apiKeyEnv)!=null,value.testStatus,value.testMessage);
    }
    public ConnectionView save(Long id,ConnectionInput input) {
        ModelConnection value=id==null?new ModelConnection():repository.findById(id).orElseThrow(() -> KnowledgeWorkspace.missing("模型连接不存在"));
        value.name=KnowledgeWorkspace.required(input.name(),"连接名称",100); value.modelId=KnowledgeWorkspace.required(input.modelId(),"模型ID",200);
        value.kind=input.kind(); if(!Set.of("CHAT","EMBEDDING","RERANK").contains(Objects.toString(value.kind,""))) throw KnowledgeWorkspace.bad("模型类型不合法");
        try {
            URI url=URI.create(KnowledgeWorkspace.required(input.baseUrl(),"服务地址",1000));
            if(!Set.of("http","https").contains(Objects.toString(url.getScheme(),"")) || url.getHost()==null || url.getUserInfo()!=null || url.getQuery()!=null || url.getFragment()!=null) throw new IllegalArgumentException();
            value.baseUrl=url.toString().replaceAll("/+$","");
        } catch(IllegalArgumentException exception) {throw KnowledgeWorkspace.bad("请填写有效 HTTP(S) 基础地址，不要携带账号密码、查询参数或片段");}
        value.apiKeyEnv=input.apiKeyEnv()==null?"":input.apiKeyEnv().trim();
        if(!value.apiKeyEnv.isEmpty() && !value.apiKeyEnv.matches("[A-Z][A-Z0-9_]{1,100}")) throw KnowledgeWorkspace.bad("密钥字段填写后端环境变量名，不填写真实密钥");
        value.testStatus="UNTESTED"; value.testMessage="配置已保存，尚未测试";
        return view(repository.save(value));
    }
    public ConnectionView test(Long id) {
        ModelConnection value=repository.findById(id).orElseThrow(() -> KnowledgeWorkspace.missing("模型连接不存在"));
        try {
            if("CHAT".equals(value.kind)) generate(value,"请简短回答。","只回答：连接成功");
            else if("EMBEDDING".equals(value.kind)) {
                Map<?,?> response=call(value,"/embeddings",Map.of("model",value.modelId,"input",List.of("校园连接测试")));
                List<?> data=(List<?>)response.get("data");
                if(data==null || data.isEmpty() || !(((Map<?,?>)data.getFirst()).get("embedding") instanceof List<?> embedding) || embedding.isEmpty()) throw new IllegalStateException();
            } else {
                Map<?,?> response=call(value,"/rerank",Map.of("model",value.modelId,"query","校园卡领取","documents",List.of("请到校园卡中心领取校园卡","图书馆提供自习座位")));
                if(!(response.get("results") instanceof List<?>)) throw new IllegalStateException();
            }
            value.testStatus="SUCCESS"; value.testMessage="已完成真实 "+value.kind+" 请求；测试可能产生服务商费用";
        } catch(Exception exception) {
            value.testStatus="FAILED"; value.testMessage=exception instanceof ResponseStatusException status?status.getReason():"模型响应结构不符合当前适配器，请检查模型ID、地址及接口兼容性";
        }
        return view(repository.save(value));
    }
    public Generated generate(ModelConnection connection,String system,String question) {
        if(!"CHAT".equals(connection.kind)) throw KnowledgeWorkspace.bad("请选择对话模型连接");
        Map<?,?> response=call(connection,"/chat/completions",Map.of("model",connection.modelId,
                "messages",List.of(Map.of("role","system","content",system),Map.of("role","user","content",question)),"temperature",0.2,"max_tokens",1200));
        try {
            var choices=(List<?>)response.get("choices"); var message=(Map<?,?>)((Map<?,?>)choices.getFirst()).get("message");
            String answer=(String)message.get("content"); if(answer==null || answer.isBlank()) throw new IllegalStateException();
            Map<?,?> usage=response.get("usage") instanceof Map<?,?> data?data:Map.of();
            return new Generated(answer,integer(usage.get("prompt_tokens")),integer(usage.get("completion_tokens")));
        } catch(Exception exception) {throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"对话模型没有返回可用文本");}
    }
    private Integer integer(Object value) {return value instanceof Number number?number.intValue():null;}
    private Map<?,?> call(ModelConnection value,String path,Map<String,?> body) {
        String key=value.apiKeyEnv==null || value.apiKeyEnv.isBlank()?null:System.getenv(value.apiKeyEnv);
        if(value.apiKeyEnv!=null && !value.apiKeyEnv.isBlank() && (key==null || key.isBlank())) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"后端未配置环境变量 "+value.apiKeyEnv+"，未发送模型请求");
        var http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build();
        var factory=new JdkClientHttpRequestFactory(http); factory.setReadTimeout(Duration.ofSeconds(35));
        var client=RestClient.builder().requestFactory(factory).baseUrl(value.baseUrl).build();
        try {
            var request=client.post().uri(path).contentType(org.springframework.http.MediaType.APPLICATION_JSON);
            if(key!=null) request.header("Authorization","Bearer "+key);
            Map<?,?> response=request.body(body).retrieve().body(Map.class);
            if(response==null) throw new IllegalStateException(); return response;
        } catch(Exception exception) {throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"模型调用失败，请检查连接、密钥、模型ID和接口兼容性；未自动重试");}
    }
    public record ConnectionInput(String name,String kind,String baseUrl,String modelId,String apiKeyEnv) {}
    public record ConnectionView(Long id,String name,String kind,String baseUrl,String modelId,String apiKeyEnv,boolean keyConfigured,String testStatus,String testMessage) {}
    public record Generated(String text,Integer inputTokens,Integer outputTokens) {}
}
