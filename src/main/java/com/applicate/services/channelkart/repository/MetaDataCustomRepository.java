package com.applicate.services.channelkart.repository;



import com.applicate.services.channelkart.models.MetaData;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author : Jinu
 * Date    : 2/8/2021
 **/
public interface MetaDataCustomRepository {

   List<MetaData> findAll(String domainName);

   MetaData findByValue(String domainName,String domainType);

   MetaData merge(MetaData metaData);

   List<MetaData> findByDomainType(String domainType);

   List<MetaData> findByDomainName(String domainName);

   MetaData deleteByValue(String domainName, String domainType) throws NoSuchElementException;

}
