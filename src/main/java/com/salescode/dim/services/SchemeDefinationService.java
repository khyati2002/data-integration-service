package com.salescode.dim.services;

import com.applicate.services.channelkart.services.AbstractCDMService;
import com.applicate.services.channelkart.services.MetaDataService;
import com.applicate.services.channelkart.utils.IDGenerator;
import com.salescode.dim.jooq.generated.Tables;
import com.salescode.dim.jooq.generated.tables.records.CkSchemeDefinationRecord;
import com.salescode.dim.jooq.impl.SchemeDefination;
import com.salescode.dim.repository.SchemeDefinationRepo;
import com.salescode.dim.repository.SchemeDefinationRepoImpl;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.function.BiFunction;

import static com.salescode.dim.jooq.generated.Tables.CK_SCHEME_DEFINATION;

public class SchemeDefinationService extends AbstractCDMService<SchemeDefination> {
    private DSLContext dsl = null;

    /**
     * The logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(SchemeDefinationService.class);

    /**
     * The repository.
     */
    private final SchemeDefinationRepo schemeDefinationRepo;
    private final SchemeProductBifurcationService schemeProductBifurcationService;
    private final SchemeLocationBifurcationService schemeLocationBifurcationService;
    private final SchemeOutletBifurcationService schemeOutletBifurcationService;
    private final SchemeCalculationService schemeCalculationService;
    private static IDGenerator idGenerator = null;
    private static MetaDataService metaDataService;
    private static final String DOMAIN_NAME = "SchemeDefination";
    private static final String DOMAIN_TYPE = "DynamicUniqueKey";

    public SchemeDefinationService() {
        this.dsl = getDslContext();
        idGenerator = new IDGenerator();
        metaDataService = new MetaDataService();
        this.schemeDefinationRepo = new SchemeDefinationRepoImpl(dsl);
        this.schemeProductBifurcationService = new SchemeProductBifurcationService();
        this.schemeLocationBifurcationService = new SchemeLocationBifurcationService();
        this.schemeOutletBifurcationService = new SchemeOutletBifurcationService();
        this.schemeCalculationService = new SchemeCalculationService();
    }

    public SchemeDefination findBySchemeId(String schemeId) {

        return schemeDefinationRepo.findBySchemeId(schemeId);
    }

    private BiFunction<com.salescode.dim.jooq.generated.tables.pojos.SchemeDefination, DSLContext, InsertSetMoreStep<?>> schemeDefinationBiFunctionMapper = (ros, dslContext) -> {
        return (InsertSetMoreStep<CkSchemeDefinationRecord>)
                dslContext.insertInto(CK_SCHEME_DEFINATION)
                        .set(Tables.CK_SCHEME_DEFINATION.ID, ros.getId())
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_STATUS, ros.getActiveStatus())
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_STATUS_REASON, ros.getActiveStatusReason())
                        .set(Tables.CK_SCHEME_DEFINATION.CHANGED, ros.getChanged())
                        .set(CK_SCHEME_DEFINATION.CREATED_BY, "flink job")
                        .set(Tables.CK_SCHEME_DEFINATION.CREATION_TIME, LocalDateTime.now(ZoneId.of("UTC")))
                        .set(CK_SCHEME_DEFINATION.EXTENDED_ATTRIBUTES, ros.getExtendedAttributes())
                        .set(Tables.CK_SCHEME_DEFINATION.HASH, ros.getHash())
                        .set(Tables.CK_SCHEME_DEFINATION.LAST_MODIFIED_TIME, LocalDateTime.now(ZoneId.of("UTC")))
                        .set(Tables.CK_SCHEME_DEFINATION.LOB, ros.getLob())
                        .set(Tables.CK_SCHEME_DEFINATION.MODIFIED_BY, ros.getModifiedBy())
                        .set(Tables.CK_SCHEME_DEFINATION.SOURCE, ros.getSource())
                        .set(Tables.CK_SCHEME_DEFINATION.VERSION, ros.getVersion())
                        .set(Tables.CK_SCHEME_DEFINATION.CALCULATION_VALUE, ros.getCalculationValue())
                        .set(Tables.CK_SCHEME_DEFINATION.CRITERIA, ros.getCriteria())
                        .set(Tables.CK_SCHEME_DEFINATION.END_DATE, ros.getEndDate().atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime())
                        .set(Tables.CK_SCHEME_DEFINATION.SCHEME_ID, ros.getSchemeId())
                        .set(Tables.CK_SCHEME_DEFINATION.PRIORITY, ros.getPriority())
                        .set(Tables.CK_SCHEME_DEFINATION.PROGRAM_LEVEL, ros.getProgramLevel())
                        .set(Tables.CK_SCHEME_DEFINATION.RANGE_LEVEL, ros.getRangeLevel())
                        .set(Tables.CK_SCHEME_DEFINATION.SCHEME_DESCRIPTION, ros.getSchemeDescription())
                        .set(Tables.CK_SCHEME_DEFINATION.SCHEME_NAME, ros.getSchemeName())
                        .set(Tables.CK_SCHEME_DEFINATION.SCHEME_TYPE, ros.getSchemeType())
                        .set(Tables.CK_SCHEME_DEFINATION.START_DATE, ros.getStartDate().atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime())
                        .set(Tables.CK_SCHEME_DEFINATION.SCHEME_CALCULATIONS, ros.getSchemeCalculations())
                        .set(Tables.CK_SCHEME_DEFINATION.STATUS, ros.getStatus())
                        .set(Tables.CK_SCHEME_DEFINATION.BANNER_URL, ros.getBannerUrl())
                        .set(Tables.CK_SCHEME_DEFINATION.BANNER_PRIORITY, ros.getBannerPriority())
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_DATES, ros.getActiveDates())
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_DAYS, ros.getActiveDays())
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_MONTHS, ros.getActiveMonths())
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_WEEKS, ros.getActiveWeeks())
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_TIME, ros.getActiveTime())
                        .onConflict(Tables.CK_SCHEME_DEFINATION.ID)
                        .doUpdate()
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_STATUS, ros.getActiveStatus())
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_STATUS_REASON, ros.getActiveStatusReason())
                        .set(Tables.CK_SCHEME_DEFINATION.CHANGED, ros.getChanged())
                        .set(CK_SCHEME_DEFINATION.EXTENDED_ATTRIBUTES, ros.getExtendedAttributes())
                        .set(Tables.CK_SCHEME_DEFINATION.HASH, ros.getHash())
                        .set(Tables.CK_SCHEME_DEFINATION.LAST_MODIFIED_TIME, LocalDateTime.now(ZoneId.of("UTC")))
                        .set(Tables.CK_SCHEME_DEFINATION.LOB, ros.getLob())
                        .set(Tables.CK_SCHEME_DEFINATION.MODIFIED_BY, ros.getModifiedBy())
                        .set(Tables.CK_SCHEME_DEFINATION.SOURCE, ros.getSource())
                        .set(Tables.CK_SCHEME_DEFINATION.VERSION, ros.getVersion())
                        .set(Tables.CK_SCHEME_DEFINATION.CALCULATION_VALUE, ros.getCalculationValue())
                        .set(Tables.CK_SCHEME_DEFINATION.CRITERIA, ros.getCriteria())
                        .set(Tables.CK_SCHEME_DEFINATION.END_DATE, ros.getEndDate().atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime())
                        .set(Tables.CK_SCHEME_DEFINATION.SCHEME_ID, ros.getSchemeId())
                        .set(Tables.CK_SCHEME_DEFINATION.PRIORITY, ros.getPriority())
                        .set(Tables.CK_SCHEME_DEFINATION.PROGRAM_LEVEL, ros.getProgramLevel())
                        .set(Tables.CK_SCHEME_DEFINATION.RANGE_LEVEL, ros.getRangeLevel())
                        .set(Tables.CK_SCHEME_DEFINATION.SCHEME_DESCRIPTION, ros.getSchemeDescription())
                        .set(Tables.CK_SCHEME_DEFINATION.SCHEME_NAME, ros.getSchemeName())
                        .set(Tables.CK_SCHEME_DEFINATION.SCHEME_TYPE, ros.getSchemeType())
                        .set(Tables.CK_SCHEME_DEFINATION.START_DATE, ros.getStartDate().atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime())
                        .set(Tables.CK_SCHEME_DEFINATION.SCHEME_CALCULATIONS, ros.getSchemeCalculations())
                        .set(Tables.CK_SCHEME_DEFINATION.STATUS, ros.getStatus())
                        .set(Tables.CK_SCHEME_DEFINATION.BANNER_URL, ros.getBannerUrl())
                        .set(Tables.CK_SCHEME_DEFINATION.BANNER_PRIORITY, ros.getBannerPriority())
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_DATES, ros.getActiveDates())
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_DAYS, ros.getActiveDays())
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_MONTHS, ros.getActiveMonths())
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_WEEKS, ros.getActiveWeeks())
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_TIME, ros.getActiveTime());
    };

    public void sdSave(SchemeDefination schemeDefination) {
        JsonNode metadata = metaDataService.fetchByValue(DOMAIN_NAME, DOMAIN_TYPE).getDomainValues();
        schemeDefination.setId(idGenerator.getIdWithMetaData(schemeDefination, metadata));
        dsl.batch(
                schemeDefinationBiFunctionMapper.apply(schemeDefination, dsl)
        ).execute();
    }

    @Override
    public Collection<SchemeDefination> batchSave(Collection<SchemeDefination> schemes) {
        for (SchemeDefination s : schemes) {
            schemeProductBifurcationService.spbSave(s.getSchemeProductBifurcationsList());
            schemeLocationBifurcationService.slbSave(s.getSchemeLocationBifurcationsList());
            schemeOutletBifurcationService.sobSave(s.getSchemeOutletBifurcationsList());
            schemeCalculationService.scSave(s.getSchemeCalculation().get(0));
            sdSave(s);

        }
        logger.info("Saved scheme defination!");

        return schemes;
    }


    @Override
    public SchemeDefination save(SchemeDefination cdmObject) {
        return null;
    }
}
