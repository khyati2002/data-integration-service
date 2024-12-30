package com.salescode.channelkart.repository;

import com.salescode.channelkart.models.GenericEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GenericEntityRepository extends CommonJpaRepository<GenericEntity, String> {
    List<GenericEntity> findByNameAndKey1AndKey2(String name, String key1, String key2);
}
