package com.pdm.resident.es;

import com.pdm.common.es.EsBaseRepository;
import com.pdm.resident.entity.Resident;
import org.springframework.stereotype.Repository;
import java.io.IOException;
import java.util.List;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

/**
 * 常住人口Elasticsearch仓储层
 * 实现常住人口信息的ES存储、查询操作，继承基础ES仓储方法
 */
@Repository
public class ResidentEsRepository extends EsBaseRepository<Resident> {

    /**
     * ES索引名称
     */
    private static final String INDEX_NAME = "pdm_resident";

    /**
     * 构造方法，注入ES客户端
     * @param esClient Elasticsearch客户端
     */
    public ResidentEsRepository(ElasticsearchClient esClient) {
        super(esClient);
    }

    /**
     * 获取ES索引名称
     * @return 索引名称
     */
    @Override
    public String getIndexName() {
        return INDEX_NAME;
    }

    /**
     * 获取文档实体类类型
     * @return Resident.class
     */
    @Override
    public Class<Resident> getDocumentClass() {
        return Resident.class;
    }

    /**
     * 多条件查询常住人口信息
     * @param name 姓名
     * @param gender 性别
     * @param nation 民族
     * @param educationLevel 文化程度
     * @param maritalStatus 婚姻状况
     * @param householdStatus 户口状态
     * @param from 起始位置
     * @param size 分页大小
     * @return 常住人口列表
     * @throws IOException ES查询异常
     */
    public List<Resident> multiConditionSearch(String name, String gender, String nation, String educationLevel,
                                               String maritalStatus, String householdStatus, int from, int size) throws IOException {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        if (name != null) {
            boolBuilder.must(Query.of(q -> q.match(m -> m.field("name").query(name))));
        }
        if (gender != null) {
            boolBuilder.must(Query.of(q -> q.term(t -> t.field("gender").value(gender))));
        }
        if (nation != null) {
            boolBuilder.must(Query.of(q -> q.term(t -> t.field("nation").value(nation))));
        }
        if (educationLevel != null) {
            boolBuilder.must(Query.of(q -> q.term(t -> t.field("educationLevel").value(educationLevel))));
        }
        if (maritalStatus != null) {
            boolBuilder.must(Query.of(q -> q.term(t -> t.field("maritalStatus").value(maritalStatus))));
        }
        if (householdStatus != null) {
            boolBuilder.must(Query.of(q -> q.term(t -> t.field("householdStatus").value(householdStatus))));
        }
        return search(boolBuilder.build()._toQuery(), from, size);
    }
}