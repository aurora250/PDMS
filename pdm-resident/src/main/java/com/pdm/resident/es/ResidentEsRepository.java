package com.pdm.resident.es;

import com.pdm.common.es.EsBaseRepository;
import com.pdm.resident.entity.Resident;

import org.springframework.stereotype.Repository;

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

    public List<Resident> multiConditionSearch(String name, String gender, String nation, String nationCode,
            String educationLevel, String educationCode, String maritalStatus, String householdStatus, int from,
            int size) throws IOException {
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
        if (nationCode != null) {
            boolBuilder.must(Query.of(q -> q.term(t -> t.field("nationCode").value(nationCode))));
        }
        if (educationLevel != null) {
            boolBuilder.must(Query.of(q -> q.term(t -> t.field("educationLevel").value(educationLevel))));
        }
        if (educationCode != null) {
            boolBuilder.must(Query.of(q -> q.term(t -> t.field("educationCode").value(educationCode))));
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
