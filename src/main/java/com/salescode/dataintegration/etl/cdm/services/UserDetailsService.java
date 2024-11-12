package com.salescode.dataintegration.etl.cdm.services;

import com.salescode.jooq.generated.tables.pojos.CkOutletDetails;
import com.salescode.jooq.generated.tables.pojos.CkUser;
import org.apache.commons.lang.StringUtils;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.salescode.jooq.generated.tables.CkOutletDetails.CK_OUTLET_DETAILS;
import static com.salescode.jooq.generated.tables.CkUser.CK_USER;

@Service
public class UserDetailsService {
    private final DSLContext dsl;

    @Autowired
    public UserDetailsService(DSLContext dsl) {
        this.dsl = dsl;
    }
    public CkUser save(CkOutletDetails outlet){

        CkUser user = new CkUser();
        user.setId(outlet.getId());
        user.setVersion(outlet.getVersion());
        user.setActiveStatus(outlet.getActiveStatus());
        user.setLoginid(outlet.getOutletcode());
        user.setUseraccountid(outlet.getOutletcode());
        user.setLocationHierarchy(outlet.getLocationHierarchy());
        user.setMobile(outlet.getContactno());
        user.setName(StringUtils.isEmpty(outlet.getOutletName()) ? outlet.getOutletcode() : outlet.getOutletName());
        user.setPassword(outlet.getOutletcode());

        var record = dsl.newRecord(CK_USER, user);

        dsl.insertInto(CK_USER)
                .set(record)
                .onDuplicateKeyUpdate()
                .set(record)
                .execute();
        return user;
    }
}
