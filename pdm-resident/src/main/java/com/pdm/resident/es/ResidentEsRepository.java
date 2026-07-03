package com.pdm.resident.es;

import com.pdm.common.es.EsBaseRepository;
import com.pdm.resident.entity.Resident;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

@Repository
public class ResidentEsRepository extends EsBaseRepository<Resident> {

    private static final String INDEX_NAME = "pdm_resident";

    public ResidentEsRepository(ElasticsearchClient esClient) {
        super(esClient);
    }

    @Override
    public String getIndexName() {
        return INDEX_NAME;
    }

    @Override
    public Class<Resident> getDocumentClass() {
        return Resident.class;
    }

    /**
     * 确保索引存在且日期字段有正确的 date 类型映射（兼容字符串和长整型时间戳）。 即使 ES 启动较晚导致 EsIndexInitializer
     * 失败，首次 save/bulkSave 也会触发此方法。
     */
    private void ensureIndexWithMapping() throws IOException {
        if (indexExists())
            return;
        synchronized (ResidentEsRepository.class) {
            if (indexExists())
                return; // double-check
            esClient.indices()
                    .create(c -> c.index(getIndexName())
                            .mappings(m -> m.properties("birthDate", p -> p.date(d -> d))
                                    .properties("createTime", p -> p.date(d -> d))
                                    .properties("updateTime", p -> p.date(d -> d))));
        }
    }

    @Override
    public void createIndex() throws IOException {
        ensureIndexWithMapping();
    }

    /** 兜底：每次保存前确保索引映射正确 */
    @Override
    public void save(String id, Resident document) throws IOException {
        ensureIndexWithMapping();
        super.save(id, document);
    }

    /** 兜底：批量保存前同样确保映射正确 */
    public void bulkSave(List<Resident> documents, java.util.function.Function<Resident, String> idExtractor)
            throws IOException {
        ensureIndexWithMapping();
        super.bulkSave(documents, idExtractor);
    }

    public List<Resident> multiConditionSearch(String name, String gender, String nation, String nationCode,
            String educationLevel, String educationCode, String maritalStatus, String householdStatus, String province,
            int from, int size) throws IOException {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        if (StringUtils.hasText(name)) {
            boolBuilder.must(Query.of(q -> q.matchPhrase(m -> m.field("name").query(name))));
        }
        if (StringUtils.hasText(gender)) {
            boolBuilder.must(Query.of(q -> q.term(t -> t.field("gender").value(gender))));
        }
        if (StringUtils.hasText(nation)) {
            boolBuilder.must(Query.of(q -> q.term(t -> t.field("nation").value(nation))));
        }
        if (StringUtils.hasText(nationCode)) {
            boolBuilder.must(Query.of(q -> q.term(t -> t.field("nationCode").value(nationCode))));
        }
        if (StringUtils.hasText(educationLevel)) {
            boolBuilder.must(Query.of(q -> q.term(t -> t.field("educationLevel").value(educationLevel))));
        }
        if (StringUtils.hasText(educationCode)) {
            boolBuilder.must(Query.of(q -> q.term(t -> t.field("educationCode").value(educationCode))));
        }
        if (StringUtils.hasText(maritalStatus)) {
            boolBuilder.must(Query.of(q -> q.term(t -> t.field("maritalStatus").value(maritalStatus))));
        }
        if (StringUtils.hasText(householdStatus)) {
            boolBuilder.must(Query.of(q -> q.term(t -> t.field("householdStatus").value(householdStatus))));
        }
        if (StringUtils.hasText(province)) {
            boolBuilder.must(Query.of(q -> q.match(m -> m.field("householdAddress").query(province))));
        }
        return search(boolBuilder.build()._toQuery(), from, size);
    }

    public long multiConditionCount(String name, String gender, String nation, String nationCode, String educationLevel,
            String educationCode, String maritalStatus, String householdStatus, String province) throws IOException {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        if (StringUtils.hasText(name)) {
            boolBuilder.must(Query.of(q -> q.matchPhrase(m -> m.field("name").query(name))));
        }
        if (StringUtils.hasText(gender)) {
            boolBuilder.must(Query.of(q -> q.term(t -> t.field("gender").value(gender))));
        }
        if (StringUtils.hasText(nation)) {
            boolBuilder.must(Query.of(q -> q.term(t -> t.field("nation").value(nation))));
        }
        if (StringUtils.hasText(nationCode)) {
            boolBuilder.must(Query.of(q -> q.term(t -> t.field("nationCode").value(nationCode))));
        }
        if (StringUtils.hasText(educationLevel)) {
            boolBuilder.must(Query.of(q -> q.term(t -> t.field("educationLevel").value(educationLevel))));
        }
        if (StringUtils.hasText(educationCode)) {
            boolBuilder.must(Query.of(q -> q.term(t -> t.field("educationCode").value(educationCode))));
        }
        if (StringUtils.hasText(maritalStatus)) {
            boolBuilder.must(Query.of(q -> q.term(t -> t.field("maritalStatus").value(maritalStatus))));
        }
        if (StringUtils.hasText(householdStatus)) {
            boolBuilder.must(Query.of(q -> q.term(t -> t.field("householdStatus").value(householdStatus))));
        }
        if (StringUtils.hasText(province)) {
            boolBuilder.must(Query.of(q -> q.match(m -> m.field("householdAddress").query(province))));
        }
        return count(boolBuilder.build()._toQuery());
    }
}
