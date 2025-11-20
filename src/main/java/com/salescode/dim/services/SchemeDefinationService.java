package com.salescode.dim.services;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.AbstractCDMService;
import com.applicate.services.channelkart.services.MetaDataService;
import com.applicate.services.channelkart.utils.IDGenerator;
import com.salescode.dim.jooq.generated.Tables;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeLocationBifurcations;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeOutletBifurcations;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeProductBifurcations;
import com.salescode.dim.jooq.generated.tables.records.CkSchemeDefinationRecord;
import com.salescode.dim.jooq.impl.SchemeCalculation;
import com.salescode.dim.jooq.impl.SchemeDefination;
import com.salescode.dim.repository.SchemeDefinationRepo;
import com.salescode.dim.repository.SchemeDefinationRepoImpl;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.jooq.UpdateSetMoreStep;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_SCHEME_DEFINATION;
import static org.jooq.meta.jaxb.OnError.LOG;

public class SchemeDefinationService extends AbstractCDMService<SchemeDefination> {
    private DSLContext dsl = null;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
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
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_STATUS, ActiveStatus.ACTIVE)
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
                        .set(Tables.CK_SCHEME_DEFINATION.END_DATE, ros.getEndDate().atZone(ZoneId.of("Asia/Kolkata")).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime())
                        .set(Tables.CK_SCHEME_DEFINATION.SCHEME_ID, ros.getSchemeId())
                        .set(Tables.CK_SCHEME_DEFINATION.PRIORITY, ros.getPriority())
                        .set(Tables.CK_SCHEME_DEFINATION.PROGRAM_LEVEL, ros.getProgramLevel())
                        .set(Tables.CK_SCHEME_DEFINATION.RANGE_LEVEL, ros.getRangeLevel())
                        .set(Tables.CK_SCHEME_DEFINATION.SCHEME_DESCRIPTION, ros.getSchemeDescription())
                        .set(Tables.CK_SCHEME_DEFINATION.SCHEME_NAME, ros.getSchemeName())
                        .set(Tables.CK_SCHEME_DEFINATION.SCHEME_TYPE, ros.getSchemeType())
                        .set(Tables.CK_SCHEME_DEFINATION.START_DATE, ros.getStartDate().atZone(ZoneId.of("Asia/Kolkata")).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime())
                        .set(Tables.CK_SCHEME_DEFINATION.SCHEME_CALCULATIONS, ros.getSchemeCalculations())
                        .set(Tables.CK_SCHEME_DEFINATION.STATUS, ros.getStatus())
                        .set(Tables.CK_SCHEME_DEFINATION.BANNER_URL, ros.getBannerUrl())
                        .set(Tables.CK_SCHEME_DEFINATION.BANNER_PRIORITY, ros.getBannerPriority())
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_DATES, ros.getActiveDates())
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_DAYS, ros.getActiveDays())
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_MONTHS, ros.getActiveMonths())
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_WEEKS, ros.getActiveWeeks())
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_TIME, ros.getActiveTime())
                        .set(CK_SCHEME_DEFINATION.CHANGED, false)
                        .onConflict(Tables.CK_SCHEME_DEFINATION.ID)
                        .doUpdate()
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_STATUS, ActiveStatus.ACTIVE)
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_STATUS_REASON, ros.getActiveStatusReason())
                        .set(Tables.CK_SCHEME_DEFINATION.CHANGED, ros.getChanged())
                        .set(CK_SCHEME_DEFINATION.CREATED_BY, "flink job")
                        .set(CK_SCHEME_DEFINATION.EXTENDED_ATTRIBUTES, ros.getExtendedAttributes())
                        .set(Tables.CK_SCHEME_DEFINATION.HASH, ros.getHash())
                        .set(Tables.CK_SCHEME_DEFINATION.LAST_MODIFIED_TIME, LocalDateTime.now(ZoneId.of("UTC")))
                        .set(Tables.CK_SCHEME_DEFINATION.LOB, ros.getLob())
                        .set(Tables.CK_SCHEME_DEFINATION.MODIFIED_BY, ros.getModifiedBy())
                        .set(Tables.CK_SCHEME_DEFINATION.SOURCE, ros.getSource())
                        .set(Tables.CK_SCHEME_DEFINATION.VERSION, ros.getVersion())
                        .set(Tables.CK_SCHEME_DEFINATION.CALCULATION_VALUE, ros.getCalculationValue())
                        .set(Tables.CK_SCHEME_DEFINATION.CRITERIA, ros.getCriteria())
                        .set(Tables.CK_SCHEME_DEFINATION.END_DATE, ros.getEndDate().atZone(ZoneId.of("Asia/Kolkata")).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime())
                        .set(Tables.CK_SCHEME_DEFINATION.SCHEME_ID, ros.getSchemeId())
                        .set(Tables.CK_SCHEME_DEFINATION.PRIORITY, ros.getPriority())
                        .set(Tables.CK_SCHEME_DEFINATION.PROGRAM_LEVEL, ros.getProgramLevel())
                        .set(Tables.CK_SCHEME_DEFINATION.RANGE_LEVEL, ros.getRangeLevel())
                        .set(Tables.CK_SCHEME_DEFINATION.SCHEME_DESCRIPTION, ros.getSchemeDescription())
                        .set(Tables.CK_SCHEME_DEFINATION.SCHEME_NAME, ros.getSchemeName())
                        .set(Tables.CK_SCHEME_DEFINATION.SCHEME_TYPE, ros.getSchemeType())
                        .set(Tables.CK_SCHEME_DEFINATION.START_DATE, ros.getStartDate().atZone(ZoneId.of("Asia/Kolkata")).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime())
                        .set(Tables.CK_SCHEME_DEFINATION.SCHEME_CALCULATIONS, ros.getSchemeCalculations())
                        .set(Tables.CK_SCHEME_DEFINATION.STATUS, ros.getStatus())
                        .set(Tables.CK_SCHEME_DEFINATION.BANNER_URL, ros.getBannerUrl())
                        .set(Tables.CK_SCHEME_DEFINATION.BANNER_PRIORITY, ros.getBannerPriority())
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_DATES, ros.getActiveDates())
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_DAYS, ros.getActiveDays())
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_MONTHS, ros.getActiveMonths())
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_WEEKS, ros.getActiveWeeks())
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_TIME, ros.getActiveTime())
                        .set(CK_SCHEME_DEFINATION.CHANGED, true);

    };


    private BiFunction<com.salescode.dim.jooq.generated.tables.pojos.SchemeDefination, DSLContext, UpdateSetMoreStep<?>> schemeDefinationEndDateMapper = (ros, dslContext) -> {

        return (UpdateSetMoreStep<CkSchemeDefinationRecord>)
                dslContext.update(CK_SCHEME_DEFINATION)
                        .set(CK_SCHEME_DEFINATION.END_DATE, ros.getEndDate().atZone(ZoneId.of("Asia/Kolkata")).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime())
                        .set(Tables.CK_SCHEME_DEFINATION.ACTIVE_STATUS, ActiveStatus.ACTIVE)
                        .set(Tables.CK_SCHEME_DEFINATION.CHANGED, true)
                        .set(CK_SCHEME_DEFINATION.CREATED_BY, "flink job")
                        .set(Tables.CK_SCHEME_DEFINATION.CREATION_TIME, LocalDateTime.now(ZoneId.of("UTC")))
                        .set(CK_SCHEME_DEFINATION.EXTENDED_ATTRIBUTES, ros.getExtendedAttributes())
                        .set(Tables.CK_SCHEME_DEFINATION.LAST_MODIFIED_TIME, LocalDateTime.now(ZoneId.of("UTC")))
                        .set(Tables.CK_SCHEME_DEFINATION.LOB, ros.getLob())
                        .set(Tables.CK_SCHEME_DEFINATION.MODIFIED_BY, ros.getModifiedBy())
                        .where(Tables.CK_SCHEME_DEFINATION.ID.eq(ros.getId()));

    };

    public void sdSave(Collection<SchemeDefination> schemeDefinations, DSLContext trxContext) {
        logger.info("updating the existing record...");
        JsonNode metadata = metaDataService.fetchByValue(DOMAIN_NAME, DOMAIN_TYPE).getDomainValues();

        trxContext.batch(schemeDefinations.stream().map(entity -> {
            entity.setId(idGenerator.getIdWithMetaData(entity, metadata));
            if(entity.getExtendedAttributes().has("onlyUpdate")){
                return schemeDefinationEndDateMapper.apply(entity,trxContext);
            }
            return schemeDefinationBiFunctionMapper.apply(entity, trxContext);
        }).collect(Collectors.toList())).execute();
//        trxContext.batch(
//
//                schemeDefinationBiFunctionMapper.apply(schemeDefination, trxContext)
//        ).execute();
    }

    public void sdSave(SchemeDefination schemeDefination, DSLContext trxContext) {
        JsonNode metadata = metaDataService.fetchByValue(DOMAIN_NAME, DOMAIN_TYPE).getDomainValues();
        schemeDefination.setId(idGenerator.getIdWithMetaData(schemeDefination, metadata));
        trxContext.batch(
                schemeDefinationBiFunctionMapper.apply(schemeDefination, trxContext)
        ).execute();
    }

    @Override
    public Collection<SchemeDefination> batchSave(Collection<SchemeDefination> schemes) {
        long currentTime = System.currentTimeMillis();
        logger.info("starting saving schemedefination...");


        List<SchemeDefination> onlyUpdateSchemes = schemes.stream()
                .filter(s -> s.getExtendedAttributes().has("onlyUpdate") &&
                        s.getExtendedAttributes().get("onlyUpdate").asBoolean())
                .collect(Collectors.toList());

        List<SchemeDefination> normalSchemes = schemes.stream()
                .filter(s -> !(s.getExtendedAttributes().has("onlyUpdate") &&
                        s.getExtendedAttributes().get("onlyUpdate").asBoolean()))
                .collect(Collectors.toList());

        if (!onlyUpdateSchemes.isEmpty()) {
            dsl.transaction(config -> {
                DSLContext trxContext = DSL.using(config);
                sdSave(onlyUpdateSchemes, trxContext);
            });
            logger.info("End date updated for schemes.");
            logger.info("Time taken for schemeDefination : {}", System.currentTimeMillis() - currentTime);
        }

        if(!normalSchemes.isEmpty()) {
            Collection<SchemeDefination> updatedIds = normalSchemes.parallelStream().map(s -> { // excecutor service
                schemeProductBifurcationService.updateWithIds(s.getSchemeProductBifurcationsList());
                schemeLocationBifurcationService.updateWithIds(s.getSchemeLocationBifurcationsList());
                schemeOutletBifurcationService.updateWithIds(s.getSchemeOutletBifurcationsList());
                schemeCalculationService.updateWithIds(s.getSchemeCalculation().get(0));
                return s;
            }).collect(Collectors.toList());
            List<SchemeProductBifurcations> schemeProductBifurcations = new ArrayList<>();
            List<SchemeLocationBifurcations> schemeLocationBifurcations = new ArrayList<>();
            List<SchemeOutletBifurcations> schemeOutletBifurcations = new ArrayList<>();
            List<SchemeCalculation> schemeCalculation = new ArrayList<>();
            for (SchemeDefination model : updatedIds) {
                schemeProductBifurcations.addAll(model.getSchemeProductBifurcationsList());
                schemeLocationBifurcations.addAll(model.getSchemeLocationBifurcationsList());
                schemeOutletBifurcations.addAll(model.getSchemeOutletBifurcationsList());
                schemeCalculation.addAll(model.getSchemeCalculation());
            }
            try {

                dsl.transaction(config -> {
                    DSLContext trxContext = DSL.using(config);
                    schemeProductBifurcationService.spbSave(schemeProductBifurcations, trxContext);
                    schemeLocationBifurcationService.slbSave(schemeLocationBifurcations, trxContext);
                    schemeOutletBifurcationService.sobSave(schemeOutletBifurcations, trxContext);
                    schemeCalculationService.scSave(schemeCalculation, trxContext);
                    sdSave(updatedIds, trxContext);
                });
                logger.info("Saved scheme defination!");
                logger.info("Time taken for schemeDefination : {}", System.currentTimeMillis() - currentTime);

            } catch (Exception e) {
                logger.error(e.getMessage(),e);
                for (SchemeDefination s : updatedIds) {

                    dsl.transaction(config -> {
                        DSLContext trxContext = DSL.using(config);
                        schemeProductBifurcationService.spbSave(s.getSchemeProductBifurcationsList(), trxContext);
                        schemeLocationBifurcationService.slbSave(s.getSchemeLocationBifurcationsList(), trxContext);
                        schemeOutletBifurcationService.sobSave(s.getSchemeOutletBifurcationsList(), trxContext);
                        schemeCalculationService.scSave(s.getSchemeCalculation(), trxContext);
                        sdSave(s, trxContext);
                    });
                }
            }
        }

        return schemes;

    }

//    private <T> void upsertEntities() {
//
//        dsl.transaction(configuration -> {
//            String entityName = entities.get(0).getClass().getSimpleName();
//            DSLContext trxContext = DSL.using(configuration);
//            try {
//                trxContext.batch(entities.stream().map(entity -> insertMapper.apply(entity, trxContext)).collect(Collectors.toList())).execute();
//                successfulCounts.merge(entityName, entities.size(), Integer::sum);
//            } catch (Exception e) {
//                log.error("Batch execution failed. Processing records individually.", e);
//                int failedCount = 0;
//                for (T entity : entities) {
//                    try {
//                        DSLContext autoCommitContext = DSL.using(context.configuration().derive(new Settings().withExecuteWithOptimisticLocking(true)));
//                        insertMapper.apply(entity, autoCommitContext).execute();
//                        successfulCounts.merge(entityName, 1, Integer::sum);
//
//                    } catch (Exception individualException) {
//                        failedCount++;
//                        log.error("Failed to upsert entity: {}", entity, individualException);
//
//                    }
//                }
//                if (failedCount > 0) {
//                    failedCounts.merge(entityName, failedCount, Integer::sum);
//                    log.warn("{} records failed during individual processing.", failedCount);
//                }
//            }
//        });


    public SchemeDefination save(SchemeDefination cdmObject, DSLContext trxContext) {
        trxContext.batch(

                schemeDefinationBiFunctionMapper.apply(cdmObject, trxContext)
        ).execute();
        return cdmObject;
    }
//    public void sdSave(Collection<SchemeDefination> schemeDefinations, DSLContext trxContext) {
//        JsonNode metadata = metaDataService.fetchByValue(DOMAIN_NAME, DOMAIN_TYPE).getDomainValues();
//
//        trxContext.batch(schemeDefinations.stream().map(entity -> {
//            entity.setId(idGenerator.getIdWithMetaData(entity, metadata));
//            return schemeDefinationBiFunctionMapper.apply(entity, trxContext);
//        }).collect(Collectors.toList())).execute();
////        trxContext.batch(
////
////                schemeDefinationBiFunctionMapper.apply(schemeDefination, trxContext)
////        ).execute();
//    }

}
