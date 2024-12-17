package com.salescode.channelkart.repository;


import com.salescode.channelkart.models.enums.ActiveStatus;
import com.salescode.channelkart.validations.RuleInfo;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface RuleRepository extends CommonJpaRepository<RuleInfo, String> {

    List<RuleInfo> findAllByType(String type);

    List<RuleInfo> findAllByTypeAndActiveStatus(String type, ActiveStatus activeStatus);

    List<RuleInfo> findAllByActiveStatus(ActiveStatus activeStatus);
}
