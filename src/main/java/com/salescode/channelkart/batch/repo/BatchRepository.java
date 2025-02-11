package com.salescode.channelkart.batch.repo;


import com.salescode.channelkart.models.CommonDataModel;
import org.checkerframework.checker.units.qual.A;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Set;

/**
 * @author : Jinu
 * Date    : 11/3/2020
 **/
@Repository
public class BatchRepository {


   @Autowired
   private DSLContext dslContext;
   public List<HashAwareData> getDuplicateHashes(Class<? extends CommonDataModel> model, Set<String> hashes) {
      String entityName = model.getSimpleName();
      Table<?> table = DSL.table(DSL.name(entityName));

      return dslContext
              .select(
                      DSL.field(DSL.name(entityName, "id"), Long.class),
                      DSL.field(DSL.name(entityName, "version"), Integer.class),
                      DSL.field(DSL.name(entityName, "hash"), String.class)
              )
              .from(table)
              .where(DSL.field(DSL.name(entityName, "hash")).in(hashes))
              .fetchInto(HashAwareData.class);
   }

}
