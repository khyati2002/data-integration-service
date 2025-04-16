package com.salescode.dim.services;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.AbstractCDMService;
import com.applicate.services.channelkart.services.MetaDataService;
import com.applicate.services.channelkart.utils.IDGenerator;
import com.salescode.dim.jooq.generated.Tables;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeLocationBifurcations;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeProductBifurcations;
import com.salescode.dim.jooq.generated.tables.records.CkSchemeLocationBifurcationsRecord;
import com.salescode.dim.repository.SchemeLocationBifurcationRepo;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_SCHEME_LOCATION_BIFURCATIONS;

public class SchemeLocationBifurcationService extends AbstractCDMService<SchemeLocationBifurcations> {
    /** The logger. */
    private static final Logger logger = LoggerFactory.getLogger(SchemeLocationBifurcationService.class);
    private static DSLContext dsl;
    private static IDGenerator idGenerator = null;
    private static MetaDataService metaDataService;
    private static final String DOMAIN_NAME = "SchemeLocationBifurcations";
    private static final String DOMAIN_TYPE = "DynamicUniqueKey";

    /** The repository. */
    private SchemeLocationBifurcationRepo schemeLocationBifurcationRepo;

    public SchemeLocationBifurcationService() {
        this.dsl = getDslContext();
        idGenerator=new IDGenerator();
        metaDataService = new MetaDataService();
    }

    public List<SchemeLocationBifurcations> findBySchemeId(String schemeId) {

        List<SchemeLocationBifurcations> schemeLocationBifurcationsList = schemeLocationBifurcationRepo.findBySchemeId(schemeId);
        if (schemeLocationBifurcationsList == null ) {
            return null;
        }
        return schemeLocationBifurcationsList;

    }
    private BiFunction<SchemeLocationBifurcations, DSLContext, InsertSetMoreStep<?>> schemeLocationBiFunctionMapper = (ros, dslContext) -> {
        return (InsertSetMoreStep<CkSchemeLocationBifurcationsRecord>)
                dslContext.insertInto(CK_SCHEME_LOCATION_BIFURCATIONS)
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.ID, ros.getId())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.ACTIVE_STATUS, ActiveStatus.ACTIVE)
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.ACTIVE_STATUS_REASON, ros.getActiveStatusReason())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.CHANGED, ros.getChanged())
                        .set(CK_SCHEME_LOCATION_BIFURCATIONS.CREATED_BY, "flink job")
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.CREATION_TIME, LocalDateTime.now(ZoneId.of("UTC")))
                        .set(CK_SCHEME_LOCATION_BIFURCATIONS.EXTENDED_ATTRIBUTES, ros.getExtendedAttributes())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.HASH, ros.getHash())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.LAST_MODIFIED_TIME, LocalDateTime.now(ZoneId.of("UTC")))
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.LOB, ros.getLob())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.MODIFIED_BY, ros.getModifiedBy())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.SOURCE, ros.getSource())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.VERSION, ros.getVersion())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.CITY, ros.getCity())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.COUNTRY, ros.getCountry())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.REGION, ros.getRegion())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.SCHEME_ID, ros.getSchemeId())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.SCHEME_LOCATION_ID, ros.getSchemeLocationId())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.STATE, ros.getState())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.TOWN, ros.getTown())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.DISTRICT, ros.getDistrict())
                        .set(CK_SCHEME_LOCATION_BIFURCATIONS.CHANGED, false)
                        .onConflict(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.ID)
                        .doUpdate()
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.ACTIVE_STATUS, ActiveStatus.ACTIVE)
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.ACTIVE_STATUS_REASON, ros.getActiveStatusReason())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.CHANGED, ros.getChanged())
                        .set(CK_SCHEME_LOCATION_BIFURCATIONS.CREATED_BY, "flink job")
                        .set(CK_SCHEME_LOCATION_BIFURCATIONS.EXTENDED_ATTRIBUTES, ros.getExtendedAttributes())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.HASH, ros.getHash())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.LAST_MODIFIED_TIME, LocalDateTime.now(ZoneId.of("UTC")))
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.LOB, ros.getLob())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.MODIFIED_BY, ros.getModifiedBy())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.SOURCE, ros.getSource())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.VERSION, ros.getVersion())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.CITY, ros.getCity())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.COUNTRY, ros.getCountry())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.REGION, ros.getRegion())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.SCHEME_ID, ros.getSchemeId())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.SCHEME_LOCATION_ID, ros.getSchemeLocationId())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.STATE, ros.getState())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.TOWN, ros.getTown())
                        .set(Tables.CK_SCHEME_LOCATION_BIFURCATIONS.DISTRICT, ros.getDistrict())
                        .set(CK_SCHEME_LOCATION_BIFURCATIONS.CHANGED, true)
                ;
    };

    public void slbSave(List<SchemeLocationBifurcations> bifurcations, DSLContext transDSL) {
        long currentTime = System.currentTimeMillis();
        JsonNode metadata = metaDataService.fetchByValue(DOMAIN_NAME, DOMAIN_TYPE).getDomainValues();
        for (SchemeLocationBifurcations slb : bifurcations) {
            slb.setId(idGenerator.getIdWithMetaData(slb, metadata));
        }
//        try {
        transDSL.batch(
                bifurcations.stream()
                        .map(spb -> schemeLocationBiFunctionMapper.apply(spb, transDSL))
                        .collect(Collectors.toList())
        ).execute();
//                } catch (Exception e) {
//            e.printStackTrace();
//        }
        logger.info("Saved Scheme Location Bifurcations");
        logger.info("Time taken for schemeProductBifurcations : {}", System.currentTimeMillis() - currentTime);

    }
    public List<SchemeLocationBifurcations> updateWithIds(List<SchemeLocationBifurcations> bifurcations){
        JsonNode metadata = metaDataService.fetchByValue(DOMAIN_NAME, DOMAIN_TYPE).getDomainValues();

        for (SchemeLocationBifurcations spb : bifurcations) {
            spb.setId(idGenerator.getIdWithMetaData(spb, metadata));
        }
        return bifurcations;
    }
    @Override
    public SchemeLocationBifurcations save(SchemeLocationBifurcations scheme) {
        return null;
    }
}
