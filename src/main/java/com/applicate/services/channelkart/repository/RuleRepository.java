package com.applicate.services.channelkart.repository;


import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.validations.RuleInfo;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface RuleRepository extends CommonJpaRepository<RuleInfo, String> {

    List<RuleInfo> findAllByType(String type);

    List<RuleInfo> findAllByTypeAndActiveStatus(String type, ActiveStatus activeStatus);

    List<RuleInfo> findAllByActiveStatus(ActiveStatus activeStatus);
}
