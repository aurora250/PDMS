package com.pdm.common.es;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;

/**
 * Elasticsearch 基础 Repository 抽象类。
 *
 * <p>
 * 封装 Elasticsearch 的通用 CRUD 操作，子类只需实现 {@link #getIndexName()} 和
 * {@link #getDocumentClass()} 方法即可获得完整的 ES 数据访问能力，包括：
 * <ul>
 * <li>单条保存、删除、按 ID 查询</li>
 * <li>条件搜索和计数</li>
 * <li>批量保存</li>
 * </ul>
 *
 * @param <T>
 *            文档类型
 */
@RequiredArgsConstructor
public abstract class EsBaseRepository<T> {

    /** Elasticsearch 客户端 */
    protected final ElasticsearchClient esClient;

    /**
     * 获取当前 Repository 对应的 ES 索引名称。
     *
     * @return 索引名称
     */
    public abstract String getIndexName();

    /**
     * 获取文档类的 Class 对象。
     *
     * @return 文档类型
     */
    public abstract Class<T> getDocumentClass();

    /**
     * 保存文檔到 ES。
     *
     * @param id
     *            文档 ID
     * @param document
     *            文档对象
     * @throws IOException
     *             IO 异常
     */
    public void save(String id, T document) throws IOException {
        esClient.index(IndexRequest.of(i -> i.index(getIndexName()).id(id).document(document)));
    }

    /**
     * 从 ES 中删除指定 ID 的文档。
     *
     * @param id
     *            文档 ID
     * @throws IOException
     *             IO 异常
     */
    public void delete(String id) throws IOException {
        esClient.delete(DeleteRequest.of(d -> d.index(getIndexName()).id(id)));
    }

    /**
     * 按 ID 查询文档。
     *
     * @param id
     *            文档 ID
     * @return 文档对象，不存在则返回 {@code null}
     * @throws IOException
     *             IO 异常
     */
    public T findById(String id) throws IOException {
        GetResponse<T> response = esClient.get(GetRequest.of(g -> g.index(getIndexName()).id(id)), getDocumentClass());
        return response.found() ? response.source() : null;
    }

    /**
     * 按条件分页搜索。
     *
     * @param query
     *            查询条件
     * @param from
     *            偏移量
     * @param size
     *            返回条数
     * @return 匹配的文档列表
     * @throws IOException
     *             IO 异常
     */
    public List<T> search(Query query, int from, int size) throws IOException {
        SearchResponse<T> response = esClient.search(
                SearchRequest.of(s -> s.index(getIndexName()).query(query).from(from).size(size)), getDocumentClass());
        return response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
    }

    /**
     * 按条件统计文档数量。
     *
     * @param query
     *            查询条件
     * @return 匹配的文档总数
     * @throws IOException
     *             IO 异常
     */
    public long count(Query query) throws IOException {
        CountResponse response = esClient.count(CountRequest.of(c -> c.index(getIndexName()).query(query)));
        return response.count();
    }

    /**
     * 批量保存文档到 ES。
     *
     * @param documents
     *            文档列表
     * @param idExtractor
     *            从文档中提取 ID 的函数
     * @throws IOException
     *             IO 异常
     */
    public void bulkSave(List<T> documents, java.util.function.Function<T, String> idExtractor) throws IOException {
        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
        for (T doc : documents) {
            String id = idExtractor.apply(doc);
            bulkBuilder.operations(op -> op.index(idx -> idx.index(getIndexName()).id(id).document(doc)));
        }
        esClient.bulk(bulkBuilder.build());
    }
}
