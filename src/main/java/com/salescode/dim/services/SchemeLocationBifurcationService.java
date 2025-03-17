package com.salescode.dim.services;

import com.applicate.services.channelkart.services.AbstractCDMService;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeLocationBifurcations;
import com.salescode.dim.repository.SchemeLocationBifurcationRepo;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_SCHEME_PRODUCT_BIFURCATIONS;

public class SchemeLocationBifurcationService extends AbstractCDMService<SchemeLocationBifurcations> {
    /** The logger. */
    private static Logger logger = LoggerFactory.getLogger(SchemeLocationBifurcationService.class);
    private static DSLContext dsl;

    /** The repository. */
    private SchemeLocationBifurcationRepo schemeLocationBifurcationRepo;

    public SchemeLocationBifurcationService(DSLContext dsl) {

        super(dsl);
        this.dsl = dsl;
    }

    public List<SchemeLocationBifurcations> findBySchemeId(String schemeId) {

        List<SchemeLocationBifurcations> locationBifurcations = schemeLocationBifurcationRepo.findBySchemeId(schemeId);
        if (locationBifurcations == null ) {
            return null;
        }
        return locationBifurcations;
    }
    public void slbSave(List<SchemeLocationBifurcations> bifurcations) {
        if (bifurcations != null) {
            bifurcations.forEach(slb -> {
                slb.setId(UUID.randomUUID().toString());
                slb.setVersion(0);
                super.addHash(slb);
            });
        }
        dsl.batchInsert(
                bifurcations.stream()
                        .map(slb -> dsl.newRecord(CK_SCHEME_PRODUCT_BIFURCATIONS, slb))
                        .collect(Collectors.toList())
        ).execute();
    }

    @Override
    public SchemeLocationBifurcations save(SchemeLocationBifurcations scheme) {
        return save(scheme);
    }
}
