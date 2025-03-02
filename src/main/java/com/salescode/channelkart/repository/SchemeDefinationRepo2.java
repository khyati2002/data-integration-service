package com.salescode.channelkart.repository;
import com.salescode.jooq.generated.tables.pojos.CkSchemeDefination;
import org.springframework.stereotype.Repository;


@Repository
public interface SchemeDefinationRepo2 {

    CkSchemeDefination findBySchemeId(String name);

}
