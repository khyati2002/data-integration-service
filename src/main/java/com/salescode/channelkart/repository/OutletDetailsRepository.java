package com.salescode.channelkart.repository;


import com.salescode.channelkart.models.OutletDetails;
import org.springframework.stereotype.Repository;

@Repository
public interface OutletDetailsRepository extends CommonJpaRepository<OutletDetails,String>{

    OutletDetails findByOutletCode(String outletCode);
}
