package com.salescode.dim.services;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.AbstractCDMService;
import com.applicate.services.channelkart.services.MetaDataService;
import com.applicate.services.channelkart.utils.IDGenerator;
import com.salescode.dim.jooq.generated.Tables;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeOutletBifurcations;
import com.salescode.dim.jooq.generated.tables.records.CkSchemeOutletBifurcationsRecord;
import com.salescode.dim.repository.SchemeOutletBifurcationRepo;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_SCHEME_OUTLET_BIFURCATIONS;

public class SchemeOutletBifurcationService extends AbstractCDMService<SchemeOutletBifurcations> {
    /** The logger. */
    private static final Logger logger = LoggerFactory.getLogger(SchemeOutletBifurcationService.class);
    private static DSLContext dsl;
    private static IDGenerator idGenerator = null;
    private static MetaDataService metaDataService;
    private static final String DOMAIN_NAME = "SchemeOutletBifurcations";
    private static final String DOMAIN_TYPE = "DynamicUniqueKey";

    /** The repository. */
    private SchemeOutletBifurcationRepo schemeOutletBifurcationRepo;

    public SchemeOutletBifurcationService() {
        this.dsl = getDslContext();
        idGenerator=new IDGenerator();
        metaDataService = new MetaDataService();
    }
    public List<SchemeOutletBifurcations> findBySchemeId(String schemeId) {
        List<SchemeOutletBifurcations> outletBifurcation = schemeOutletBifurcationRepo.findBySchemeId(schemeId);
        if (outletBifurcation == null ) {
            return null;
        }
        return outletBifurcation;
    }
    private BiFunction<SchemeOutletBifurcations, DSLContext, InsertSetMoreStep<?>> schemeOutletBiFunctionMapper = (ros, dslContext) -> {
        return (InsertSetMoreStep<CkSchemeOutletBifurcationsRecord>)
                dslContext.insertInto(CK_SCHEME_OUTLET_BIFURCATIONS)
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.ID, ros.getId())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.ACTIVE_STATUS, ActiveStatus.ACTIVE)
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.ACTIVE_STATUS_REASON, ros.getActiveStatusReason())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.CHANGED, ros.getChanged())
                        .set(CK_SCHEME_OUTLET_BIFURCATIONS.CREATED_BY, "flink job")
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.CREATION_TIME, LocalDateTime.now(ZoneId.of("UTC")))
                        .set(CK_SCHEME_OUTLET_BIFURCATIONS.EXTENDED_ATTRIBUTES, ros.getExtendedAttributes())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.HASH, ros.getHash())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.LAST_MODIFIED_TIME, LocalDateTime.now(ZoneId.of("UTC")))
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.LOB, ros.getLob())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.MODIFIED_BY, ros.getModifiedBy())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.SOURCE, ros.getSource())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.VERSION, ros.getVersion())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.CHANNEL, ros.getChannel())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.LOGIN_ID, ros.getLoginId())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.OUTLET_CATEGORY, ros.getOutletCategory())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.SCHEME_ID, ros.getSchemeId())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.OUTLET_CODE, ros.getOutletCode())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.OUTLET_TYPE, ros.getOutletType())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.SUB_CHANNEL, ros.getSubChannel())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.DISTRIBUTION_CHANNEL, ros.getDistributionChannel())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.ACCOUNT, ros.getAccount())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.OUTLET_CLASS, ros.getOutletClass())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.MARKET_ID, ros.getMarketId())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.MARKET_NAME, ros.getMarketName())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.SUB_TERRITORY, ros.getSubTerritory())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.SOLD_TO, ros.getSoldTo())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.OUTLET_DIVISION, ros.getOutletDivision())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.PRICE_LIST_ID, ros.getPriceListId())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.MARKET_END_DATE, ros.getMarketEndDate())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.MARKET_START_DATE, ros.getMarketStartDate())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.BEAT, ros.getBeat())
                        .onConflict(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.ID)
                        .doUpdate()
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.ACTIVE_STATUS, ActiveStatus.ACTIVE)
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.ACTIVE_STATUS_REASON, ros.getActiveStatusReason())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.CHANGED, ros.getChanged())
                        .set(CK_SCHEME_OUTLET_BIFURCATIONS.CREATED_BY, "flink job")
                        .set(CK_SCHEME_OUTLET_BIFURCATIONS.EXTENDED_ATTRIBUTES, ros.getExtendedAttributes())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.HASH, ros.getHash())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.LAST_MODIFIED_TIME, LocalDateTime.now(ZoneId.of("UTC")))
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.LOB, ros.getLob())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.MODIFIED_BY, ros.getModifiedBy())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.SOURCE, ros.getSource())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.VERSION, ros.getVersion())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.CHANNEL, ros.getChannel())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.LOGIN_ID, ros.getLoginId())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.OUTLET_CATEGORY, ros.getOutletCategory())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.SCHEME_ID, ros.getSchemeId())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.OUTLET_CODE, ros.getOutletCode())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.OUTLET_TYPE, ros.getOutletType())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.SUB_CHANNEL, ros.getSubChannel())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.DISTRIBUTION_CHANNEL, ros.getDistributionChannel())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.ACCOUNT, ros.getAccount())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.OUTLET_CLASS, ros.getOutletClass())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.MARKET_ID, ros.getMarketId())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.MARKET_NAME, ros.getMarketName())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.SUB_TERRITORY, ros.getSubTerritory())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.SOLD_TO, ros.getSoldTo())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.OUTLET_DIVISION, ros.getOutletDivision())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.PRICE_LIST_ID, ros.getPriceListId())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.MARKET_END_DATE, ros.getMarketEndDate())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.MARKET_START_DATE, ros.getMarketStartDate())
                        .set(Tables.CK_SCHEME_OUTLET_BIFURCATIONS.BEAT, ros.getBeat());
    };
    public void sobSave(List<SchemeOutletBifurcations> bifurcations) {
        JsonNode metadata = metaDataService.fetchByValue(DOMAIN_NAME, DOMAIN_TYPE).getDomainValues();
        if (bifurcations != null) {
            bifurcations.forEach(sob -> {
                sob.setId(UUID.randomUUID().toString());
                sob.setVersion(0);
                super.addHash(sob);
            });
        }
        for (SchemeOutletBifurcations spb : bifurcations) {
            spb.setId(idGenerator.getIdWithMetaData(spb, metadata));
        }
        dsl.batch(
                bifurcations.stream()
                        .map(sob -> schemeOutletBiFunctionMapper.apply(sob,dsl))
                        .collect(Collectors.toList())
        ).execute();
        logger.info("SchemeOutletBifurcations saved!!!");
    }

    @Override
    public SchemeOutletBifurcations save(SchemeOutletBifurcations scheme) {
        return null;
    }
}
