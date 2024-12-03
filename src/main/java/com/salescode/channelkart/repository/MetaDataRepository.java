package com.salescode.channelkart.repository;


import com.salescode.jooq.generated.tables.pojos.CkMetadata;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface MetaDataRepository extends MetaDataCustomRepository {

    Optional<CkMetadata> findByDomainNameAndDomainType(String domainName, String domainType);

}
