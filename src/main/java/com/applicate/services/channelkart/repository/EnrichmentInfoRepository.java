/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.applicate.services.channelkart.repository;

import com.applicate.services.channelkart.enrichments.EnrichmentInfo;
import com.applicate.services.channelkart.enrichments.EnrichmentPhase;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * The interface EnrichmentInfoRepository.
 *
 * @author Manish Srivastava
 * @since May 2020
 */
@Repository
public interface EnrichmentInfoRepository extends CommonJpaRepository<EnrichmentInfo, String> {

    /**
     * Find all.
     *
     * @return the list
     */
    List<EnrichmentInfo> findAll();

    List<EnrichmentInfo> findByPhase(@NotNull EnrichmentPhase phase);

    List<EnrichmentInfo> findByPhaseAndActiveStatus(@NotNull EnrichmentPhase phase, ActiveStatus activeStatus);

    List<EnrichmentInfo> findAllByActiveStatus(ActiveStatus activeStatus);
}
