package com.salescode.dim.services;

import com.applicate.services.channelkart.services.AbstractCDMService;
import com.salescode.dim.jooq.generated.tables.CkSchemeProductBifurcations;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeDefination;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeProductBifurcations;
import com.salescode.dim.repository.SchemeProductBifurcationRepo;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_SCHEME_PRODUCT_BIFURCATIONS;
import static com.salescode.dim.jooq.generated.Tables.CK_USER_PARENT;

public class SchemeProductBifurcationService extends AbstractCDMService<SchemeProductBifurcations> {

    /** The logger. */
    private static Logger logger = LoggerFactory.getLogger(SchemeProductBifurcationService.class);
    private static DSLContext dsl;

    /** The repository. */
    private SchemeProductBifurcationRepo schemeProductBifurcationRepo;

    public SchemeProductBifurcationService(DSLContext dsl) {
        super(dsl);
        this.dsl = dsl;
    }

    public List<SchemeProductBifurcations> findBySchemeId(String schemeId) {

        List<SchemeProductBifurcations> schemeProductBifurcationsList = schemeProductBifurcationRepo.findBySchemeId(schemeId);
        if (schemeProductBifurcationsList == null ) {
            return null;
        }
        return schemeProductBifurcationsList;

    }

    public void spbSave(List<SchemeProductBifurcations> bifurcations) {
        if (bifurcations != null) {
            bifurcations.forEach(spb -> {
                spb.setId(UUID.randomUUID().toString());
                spb.setVersion(0);
                super.addHash(spb);
            });
        }
        dsl.batchInsert(
                bifurcations.stream()
                        .map(spb -> dsl.newRecord(CK_SCHEME_PRODUCT_BIFURCATIONS, spb))
                        .collect(Collectors.toList())
        ).execute();
    }

    @Override
    public SchemeProductBifurcations save(SchemeProductBifurcations scheme) {
        return null;
    }
}
