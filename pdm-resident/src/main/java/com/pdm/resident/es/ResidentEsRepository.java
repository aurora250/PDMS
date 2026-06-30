package com.pdm.resident.es;

import com.pdm.common.es.EsBaseRepository;
import com.pdm.resident.entity.Resident;

import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import org.springframework.util.StringUtils;

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

    public List<Resident> multiConditionSearch(String name, String gender, String nation, String nationCode,
            String educationLevel, String educationCode, String maritalStatus, String householdStatus,
            String province, int from, int size) throws IOException {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        if (StringUtils.hasText(name)) {
            boolBuilder.must(Query.of(q -> q.match(m -> m.field("name").query(name))));
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
            boolBuilder.must(Query.of(q -> q.match(m -> m.field("name").query(name))));
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
