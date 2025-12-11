package com.applicate.services.channelkart.utils;

import org.jooq.*;
import org.jooq.impl.DSL;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BatchInsertUtil {
    public static <T, K1, K2> void saveBatchWithDuplicateCheck(
            DSLContext dsl,
            List<T> items,
            Table<?> table,
            Function<T, Row2<K1, K2>> keyExtractor,
            Field<K1> field1,
            Field<K2> field2,
            Function<T, Query> queryBuilder  // Changed from InsertSetStep<?> to Query
    ) {
        // 1. Collect keys from the input list
        Set<Row2<K1, K2>> keysToInsert = items.stream()
                .map(keyExtractor)
                .collect(Collectors.toSet());

        if (keysToInsert.isEmpty()) {
            return;
        }

        // 2. Fetch existing keys from the database
        Result<Record2<K1, K2>> result = dsl
                .select(field1, field2)
                .from(table)
                .where(DSL.row(field1, field2).in(keysToInsert))
                .fetch();

        // 3. Convert to set for lookup
        Set<Row2<K1, K2>> existingKeySet = result.stream()
                .map(record -> DSL.row(record.get(field1), record.get(field2)))
                .collect(Collectors.toSet());

        // 4. Filter out existing records
        List<T> newItems = items.stream()
                .filter(item -> !existingKeySet.contains(keyExtractor.apply(item)))
                .collect(Collectors.toList());

        // 5. Create and execute batch insert
        if (!newItems.isEmpty()) {
            List<Query> queries = newItems.stream()
                    .map(queryBuilder)
                    .collect(Collectors.toList());

            dsl.batch(queries).execute();
        }
    }
}
