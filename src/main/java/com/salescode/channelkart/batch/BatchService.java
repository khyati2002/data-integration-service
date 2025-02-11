package com.salescode.channelkart.batch;


import java.util.List;
import java.util.Map;
import java.util.Set;

import com.salescode.channelkart.batch.hash.BatchContainer;
import com.salescode.channelkart.batch.repo.BatchRepository;
import com.salescode.channelkart.batch.repo.HashAwareData;
import com.salescode.channelkart.models.CommonDataModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author : Jinu
 * Date    : 11/2/2020
 **/
@Service
public class BatchService {

   @Autowired
   private BatchRepository batchRepository;

   public <T extends CommonDataModel> T addHashIfPresent(T model) {
      String hash = model.hash();
      model.setHash(hash);
      return model;
   }

   public <T extends CommonDataModel> BatchContainer<T> splitElements(Map<String, T> hashedElements) {
      Class<? extends CommonDataModel> aClass = hashedElements.values().stream().findFirst().orElseThrow(() -> new IllegalArgumentException("cant get entity type from empty map")).getClass();
      Set<String> strings = hashedElements.keySet();
      List<HashAwareData> duplicateHashes = batchRepository.getDuplicateHashes(aClass, strings);
      BatchContainer<T> container = new BatchContainer<>();
      duplicateHashes.forEach(data -> {
         T element = hashedElements.remove(data.getHash());
         if(element!=null) {
            element.setId(data.getId());
            element.setVersion(data.getVersion());
            container.addToDuplicate(element);
         }
      });
      container.setElementsToUpdate(hashedElements.values());
      return container;
   }
}
