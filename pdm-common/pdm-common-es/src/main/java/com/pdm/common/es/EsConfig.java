package com.pdm.common.es;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;

/**
 * Elasticsearch 客户端配置。 注册 JavaTimeModule 以支持 LocalDate / LocalDateTime 的
 * Jackson 序列化。
 */
@Configuration
public class EsConfig {

    @Value("${spring.elasticsearch.uris:http://127.0.0.1:9200}")
    private String uris;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // 解析 URI
        String uri = uris.split(",")[0].trim();
        HttpHost host = HttpHost.create(uri);

        RestClient restClient = RestClient.builder(host).build();

        // 配置 Jackson ObjectMapper，注册 JavaTimeModule 处理 LocalDate/LocalDateTime
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(mapper);
        RestClientTransport transport = new RestClientTransport(restClient, jsonpMapper);

        return new ElasticsearchClient(transport);
    }
}
