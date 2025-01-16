package com.applicate.services.channelkart.batch;

import com.applicate.services.channelkart.batch.repo.BatchRepository;
import com.applicate.services.channelkart.models.CommonDataModel;
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
}
