package com.salescode.dis.insights.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class JobSummaryCustomRepositoryImpl implements JobSummaryCustomRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Map<String, Object>> getFilteredLobSummary(List<String> lobs, Map<String, String> jobFilters) {
        StringBuilder sql = new StringBuilder("""
            SELECT igj.status, igj.master, igj.start_time, igj.end_time, igj.id AS job_id,
                   igf.id AS file_id, igj.lob, igf.consumed_fail_count,
                   igf.consumed_success_count, igf.total_count
            FROM integration_job igj
            LEFT JOIN integration_file igf ON igj.id = igf.job_id
            WHERE 1=1
        """);

        if (lobs != null && !lobs.isEmpty()) {
            sql.append(" AND igj.lob IN :lobs");
        }

        for (String key : jobFilters.keySet()) {
            sql.append(" AND igj.").append(key).append(" = :").append(key);
        }

        Query query = entityManager.createNativeQuery(sql.toString());

        if (lobs != null && !lobs.isEmpty()) {
            query.setParameter("lobs", lobs);
        }

        for (Map.Entry<String, String> entry : jobFilters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }

        query.unwrap(org.hibernate.query.NativeQuery.class)
             .setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);

        return query.getResultList();
    }

    @Override
    public List<Map<String, Object>> getFilteredLobDetails(List<String> lobs, Map<String, String> jobFilters) {
        StringBuilder sql = new StringBuilder("""
            SELECT igj.lob,
                   COUNT(*) AS total,
                   COUNT(DISTINCT igj.master) AS distinct_master_count,
                   SUM(CASE WHEN igj.status = 'COMPLETED' THEN 1 ELSE 0 END) AS COMPLETED,
                   SUM(CASE WHEN igj.status = 'PENDING' THEN 1 ELSE 0 END) AS PENDING,
                   SUM(CASE WHEN igj.status = 'FAILED' THEN 1 ELSE 0 END) AS FAILED
            FROM integration_job igj
            WHERE 1=1
        """);

        if (lobs != null && !lobs.isEmpty()) {
            sql.append(" AND igj.lob IN :lobs");
        }

        for (String key : jobFilters.keySet()) {
            sql.append(" AND igj.").append(key).append(" = :").append(key);
        }

        sql.append(" GROUP BY igj.lob");

        Query query = entityManager.createNativeQuery(sql.toString());

        if (lobs != null && !lobs.isEmpty()) {
            query.setParameter("lobs", lobs);
        }

        for (Map.Entry<String, String> entry : jobFilters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }

        query.unwrap(org.hibernate.query.NativeQuery.class)
             .setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);

        return query.getResultList();
    }
}
