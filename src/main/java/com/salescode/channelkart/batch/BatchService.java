package com.salescode.channelkart.batch;

import com.salescode.channelkart.batch.hash.BatchContainer;
import com.salescode.channelkart.batch.repo.BatchRepository;
import com.salescode.channelkart.batch.repo.HashAwareData;
import com.salescode.channelkart.models.CommonDataModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
