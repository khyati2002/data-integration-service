package com.salescode.channelkart.repository;

import com.salescode.channelkart.models.MetaData;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface MetaDataRepository extends CommonJpaRepository<MetaData,String>, MetaDataCustomRepository {

    @Transactional(propagation = Propagation.NOT_SUPPORTED )
    Optional<MetaData> findByDomainNameAndDomainType(String domainName, String domainType);

}
