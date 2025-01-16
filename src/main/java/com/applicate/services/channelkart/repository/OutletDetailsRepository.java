package com.applicate.services.channelkart.repository;


import com.applicate.services.channelkart.models.OutletDetails;
import org.springframework.stereotype.Repository;

@Repository
public interface OutletDetailsRepository extends CommonJpaRepository<OutletDetails,String>{

    OutletDetails findByOutletCode(String outletCode);
}
