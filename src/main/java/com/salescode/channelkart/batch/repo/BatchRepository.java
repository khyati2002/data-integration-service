package com.salescode.channelkart.batch.repo;

import com.salescode.channelkart.models.CommonDataModel;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Set;

/**
 * @author : Jinu
 * Date    : 11/3/2020
 **/
@Repository
public class BatchRepository {

   @PersistenceContext
   private EntityManager entityManager;

   public List<HashAwareData> getDuplicateHashes(Class<? extends CommonDataModel> model, Set<String> hashes) {
      String entityName = model.getSimpleName();
      String query = "select new com.salescode.channelkart.batch.repo.HashAwareData(m.id, m.version, m.hash) from %s m where m.hash in :hashes";
      query = String.format(query, entityName);
      return entityManager.createQuery(query, HashAwareData.class)
              .setParameter("hashes",hashes)
              .getResultList();
   }

}
