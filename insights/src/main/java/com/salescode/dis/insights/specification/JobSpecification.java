package com.salescode.dis.insights.specification;

import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.JobEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.persistence.criteria.Predicate;

public class JobSpecification {

    public static Specification<JobEntity> getJobWithFileSpecification(
            List<String> lob,
            Map<String, String> jobFilters,
            Map<String, String> fileFilters
    ) {
        return (root, query, builder) -> {
            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            if (lob != null && !lob.isEmpty()) {
                predicates.add(root.get("lob").in(lob));
            }

            if (jobFilters != null && !jobFilters.isEmpty()) {
                for (Map.Entry<String, String> entry : jobFilters.entrySet()) {
                    predicates.add(
                            builder.equal(
                                    builder.lower(root.get(entry.getKey())),
                                    entry.getValue().toLowerCase()
                            )
                    );
                }
            }

            LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
            predicates.add(builder.greaterThanOrEqualTo(root.get("lastModifiedTime"), threeDaysAgo));

            if (fileFilters != null && !fileFilters.isEmpty()) {
                Join<JobEntity, FileEntity> fileJoin = root.join("files", JoinType.INNER);

                for (Map.Entry<String, String> entry : fileFilters.entrySet()) {
                    predicates.add(
                            builder.equal(
                                    builder.lower(fileJoin.get(entry.getKey())),
                                    entry.getValue().toLowerCase()
                            )
                    );
                }
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
