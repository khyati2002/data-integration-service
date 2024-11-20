package com.salescode.dataintegration.etl.cdm.repository;

import com.salescode.jooq.generated.tables.pojos.CkMetadata;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author : Jinu
 * Date    : 2/8/2021
 **/
@Repository
public interface MetaDataCustomRepository {

   List<CkMetadata> findAll(String domainName);

   CkMetadata findByValue(String domainName,String domainType);

   CkMetadata merge(CkMetadata metaData);

   List<CkMetadata> findByDomainType(String domainType);

   List<CkMetadata> findByDomainName(String domainName);

   CkMetadata deleteByValue(String domainName, String domainType);
//         throws NoSuchElementException;

}
