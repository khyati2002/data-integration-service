package com.salescode.channelkart.cache;

import com.salescode.channelkart.abstractdatasource.DatabaseProfileRegistry;
import com.salescode.channelkart.client.properties.PropertyRegistry;
import com.salescode.channelkart.enrichments.EnrichmentRegistry;
import com.salescode.channelkart.profiles.ProfileRegistry;
import com.salescode.channelkart.scanner.ExternalRegistryScanner;
import com.salescode.channelkart.services.SpringContext;
import com.salescode.channelkart.transformers.TransformerRegistry;
import com.salescode.channelkart.validations.RuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Service
public class StartupBooster {
    public static final String SYSTEM_INFO = "systemInfo";
    private static final String SYSTEM_TYPE = "systemType";
    private static final List<String> inList = new ArrayList<>();
    private static final boolean isSystemHealthy = true;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    DecimalFormat df = new DecimalFormat("#.00");
    private final DatabaseProfileRegistry databaseProfileRegistry;
    private final DistributedCache cacheManager;
    @Autowired
    private Environment env;
    @Autowired ProfileRegistry profileRegistry;
    @Autowired PropertyRegistry propertyRegistry;
    @Value("${dbUpgrade:false}")
    private boolean isDbUpgrade;
    private final String systemType;
    private double lastCpu;

    public StartupBooster(DatabaseProfileRegistry databaseProfileRegistry, SpringContext springContext, DistributedCache cacheManager) {
        this.databaseProfileRegistry = databaseProfileRegistry;
        this.cacheManager = cacheManager;
        if (springContext != null) {
            logger.info("initilaized the Spring context");
        }
        systemType = Optional.ofNullable(System.getenv(SYSTEM_TYPE)).orElse(System.getProperty(SYSTEM_TYPE, "unknown"));
    }

    @PostConstruct
    public void init() {
        if (isDbUpgrade) {
            // this is just for db upgrade no need to initialize the services
            return;
        }
        load(null);
        initExtraServices();
        logger.info("Initialized StartupBooster");
    }

    public void load(Predicate<String> predicate) {
        try {
            EnrichmentRegistry.INSTANCE.loadAll(predicate, Boolean.TRUE);
            RuleRegistry.INSTANCE.loadAll(predicate, Boolean.TRUE);
            TransformerRegistry.INSTANCE.loadAll(predicate, Boolean.TRUE);
            ProfileRegistry.INSTANCE.loadAll(predicate, Boolean.TRUE);

            logger.info("finished loading registry");
        } catch (Exception e) {
            logger.error("Could not load the registries", e);
        }
    }

    private void initExtraServices() {
        try {
            ExternalRegistryScanner.getInstance().loadAll(null);

        } catch (Exception e) {
            logger.error("Extra services initialization failed", e);
        }
    }



}

