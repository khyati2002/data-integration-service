package com.salescode.dim.services;
import com.applicate.services.channelkart.services.AbstractCDMService;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeOutletBifurcations;
import com.salescode.dim.repository.SchemeOutletBifurcationRepo;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_SCHEME_OUTLET_BIFURCATIONS;
import static com.salescode.dim.jooq.generated.Tables.CK_SCHEME_PRODUCT_BIFURCATIONS;

public class SchemeOutletBifurcationService extends AbstractCDMService<SchemeOutletBifurcations> {
    /** The logger. */
    private static Logger logger = LoggerFactory.getLogger(SchemeOutletBifurcationService.class);
    private static DSLContext dsl;

    /** The repository. */
    private SchemeOutletBifurcationRepo schemeOutletBifurcationRepo;

    public SchemeOutletBifurcationService(DSLContext dsl) {

        super(dsl);
        this.dsl = dsl;
    }

    public List<SchemeOutletBifurcations> findBySchemeId(String schemeId) {

        List<SchemeOutletBifurcations> outletBifurcation = schemeOutletBifurcationRepo.findBySchemeId(schemeId);
        if (outletBifurcation == null ) {
            return null;
        }
        return outletBifurcation;

    }
    public void sobSave(List<SchemeOutletBifurcations> bifurcations) {
        if (bifurcations != null) {
            bifurcations.forEach(sob -> {
                sob.setId(UUID.randomUUID().toString());
                sob.setVersion(0);
                super.addHash(sob);
            });
        }
        dsl.batchInsert(
                bifurcations.stream()
                        .map(sob -> dsl.newRecord(CK_SCHEME_OUTLET_BIFURCATIONS, sob))
                        .collect(Collectors.toList())
        ).execute();
    }

    @Override
    public SchemeOutletBifurcations save(SchemeOutletBifurcations scheme) {
        return null;
    }
}
