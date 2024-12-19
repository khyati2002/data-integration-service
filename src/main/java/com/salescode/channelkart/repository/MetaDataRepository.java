package com.salescode.channelkart.repository;

import com.salescode.channelkart.models.MetaData;
import com.salescode.channelkart.models.enums.ActiveStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetaDataRepository extends CommonJpaRepository<MetaData,String>, MetaDataCustomRepository {

    @Transactional(propagation = Propagation.NOT_SUPPORTED )
    Optional<MetaData> findByDomainNameAndDomainType(String domainName, String domainType);

    @Query("select m.id from MetaData m where m.domainName=?1 and m.domainType=?2 and m.activeStatus='active'")
    String getIdByDomainNameAndDomainType(String domainName, String domainType);

    MetaData findByIdAndActiveStatus(String id, ActiveStatus activeStatus);

    List<MetaData> findAllByActiveStatus(ActiveStatus activeStatus);
}
