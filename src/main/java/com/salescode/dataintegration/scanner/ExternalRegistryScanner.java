package com.salescode.dataintegration.scanner;

import com.salescode.dataintegration.etl.interfaces.TypeAwareEtlStep;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
public class ExternalRegistryScanner {

    // Cache for storing class instances
    private final Map<String, ? extends TypeAwareEtlStep> instanceCache = new HashMap<>();

    private final transient DSLContext dslContext;

    public ExternalRegistryScanner(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    /**
     * Loads and caches classes from a bundle jar file based on the LOB.
     *
     * @param lob the LOB of the client.
     */
    public void loadClassesFromLob(String lob) {
        // Fetch profile from the database
        Record profile = fetchProfile(lob);

        if (profile == null) {
            throw new RuntimeException("Bundle Profile not found for LOB: " + lob);
        }

        String s3JarUrl = profile.getValue("artifactURL", String.class); // Fetch the S3 URL from profile
        JarScanner jarScanner = new JarScanner();
        jarScanner.loadAndCacheClasses(s3JarUrl, instanceCache);
    }

    /**
     * Fetch profile record from the database using Jooq based on the LOB.
     */
    private Record fetchProfile(String lob) {
        return dslContext.select()
                .from("profile")
                .where("lob = ? AND type = 'bundle'", lob)
                .fetchOne();
    }

    /**
     * Retrieve a cached instance of a class by its name.
     */
    public Object getCachedInstance(String className) {
        return instanceCache.get(className);
    }

    public Collection< ? extends TypeAwareEtlStep> getInstances(){
        return instanceCache.values();
    }
}