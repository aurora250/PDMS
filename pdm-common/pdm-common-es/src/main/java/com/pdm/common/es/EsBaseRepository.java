package com.pdm.common.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract class EsBaseRepository<T> {

    protected final ElasticsearchClient esClient;

    public abstract String getIndexName();

    public abstract Class<T> getDocumentClass();

    public void save(String id, T document) throws IOException {
        esClient.index(IndexRequest.of(i -> i.index(getIndexName()).id(id).document(document)));
    }

    public void delete(String id) throws IOException {
        esClient.delete(DeleteRequest.of(d -> d.index(getIndexName()).id(id)));
    }

    public T findById(String id) throws IOException {
        GetResponse<T> response =
                esClient.get(
                        GetRequest.of(g -> g.index(getIndexName()).id(id)), getDocumentClass());
        return response.found() ? response.source() : null;
    }

    public List<T> search(Query query, int from, int size) throws IOException {
        SearchResponse<T> response =
                esClient.search(
                        SearchRequest.of(
                                s ->
                                        s.index(getIndexName())
                                                .query(query)
                                                .from(from)
                                                .size(size)),
                        getDocumentClass());
        return response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());
    }

    public long count(Query query) throws IOException {
        CountResponse response =
                esClient.count(CountRequest.of(c -> c.index(getIndexName()).query(query)));
        return response.count();
    }

    public void bulkSave(List<T> documents, java.util.function.Function<T, String> idExtractor)
            throws IOException {
        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
        for (T doc : documents) {
            String id = idExtractor.apply(doc);
            bulkBuilder.operations(
                    op -> op.index(idx -> idx.index(getIndexName()).id(id).document(doc)));
        }
        esClient.bulk(bulkBuilder.build());
    }
}
