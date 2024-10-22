package com.salescode.channelkart.registry;

import com.salescode.channelkart.exceptions.CustomRuntimeException;
import com.salescode.channelkart.models.Profile;
import com.salescode.dataintegration.DataIntegrationApplication;
import org.jooq.DSLContext;
import org.jooq.Param;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Component
public class ProfileRegistry extends AbstractRegistry<Profile> implements InitializingBean {

    //public static final ProfileRegistry INSTANCE = new ProfileRegistry();
    public static ProfileRegistry INSTANCE;

    private static final List<Profile> EMPTY_PROFILES=Collections
            .unmodifiableList(new ArrayList<>());
//    private DistributedCache distributedCache =null;

    private final DSLContext dslContext;
    private final EntityManager entityManager;

    Map<String,List<Profile>> profilesCache = new ConcurrentHashMap<>();

    private static final String CACHE_DOMAIN = "profile";

    public ProfileRegistry(DSLContext dslContext, EntityManager entityManager) {
	/*	try {
			distributedCache = SpringContext.getBean(DistributedCache.class);
		}catch (Exception e) {
			log.error("stacktrace", e);
		}
	*/

        this.dslContext= dslContext;
        this.entityManager = entityManager;
    }

//    public DistributedCache getDistributedCache() {
//        return distributedCache;
//    }

//    public void setDistributedCache(DistributedCache distributedCache) {
//        this.distributedCache = distributedCache;
//    }

    @Override
    public void add(Profile profile) {
//        synchronized (profile.getLob().intern()) {
//            List<Profile> rules = (List<Profile>) distributedCache.get(profile.getLob(), null, CACHE_DOMAIN, false);
//            if (rules == null) {
//                rules = new ArrayList<Profile>();
//            }
//            rules.add(profile);
//            clear(profile.getLob());
//            distributedCache.put(profile.getLob(), null, CACHE_DOMAIN, rules, false);
//        }
        throw new CustomRuntimeException("Not Implemented");
    }

    @Override
    public List<Profile> get(String lob) {
//        List<Profile> profiles = distributedCache.withCache(lob, null, CACHE_DOMAIN,
//                (s)->AllLOBRouter.loadAll(Profile.class,(k) -> k.equalsIgnoreCase(lob)).stream().map(e->(Profile)e).collect(Collectors.toList()));
//        List<Profile> profiles = AllLOBRouter.loadAll(Profile.class,(k) -> k.equalsIgnoreCase(lob)).stream().map(e->(Profile)e).collect(Collectors.toList());
        return lob != null && profilesCache != null && profilesCache.containsKey(lob) ? profilesCache.get(lob) : EMPTY_PROFILES;
    }

    @Override
    public List<Profile> get(String lob, String type) {
        return get(lob,(rule)->rule.getType().equalsIgnoreCase(type));

    }

    public List<Profile> get(String lob, String type, String  name) {
//        return AppCacheManager.getInstance().withCache(lob+":"+type,name,(s)-> get(lob,rule->(rule.getType().equalsIgnoreCase(type) && rule.getName().equals(name))));
        return get(lob,rule->(rule.getType().equalsIgnoreCase(type) && rule.getName().equals(name)));
    }

    @Override
    public void addAll(String lob,List<Profile> profile) {
//        synchronized (lob.intern()) {
//            clear(lob);
//            distributedCache.put(lob, null, CACHE_DOMAIN, profile, false);
//        }
        if(profilesCache.containsKey(lob))
            profilesCache.get(lob).addAll(profile);
        else throw new IllegalArgumentException("Invalid lob profile");
    }

    @Override
    public void loadAll(Predicate<String> predicate,boolean overrideOld) {
//        AllLOBRouter.loadAll(Profile.class,predicate).stream().map(e->(Profile)e).collect(Collectors.groupingBy(a->a.getLob())).
//                entrySet().stream().forEach(e->{
////                    if(distributedCache.get(e.getKey(), null, CACHE_DOMAIN, false)==null || overrideOld)
//                        addAll(e.getKey(),e.getValue());
//                });
        List<Profile> profilesList =
                nativeQuery(entityManager,
                        dslContext.query("select * from profile")
                        , Profile.class);
        addAll(DataIntegrationApplication.getLob(),profilesList);
    }

    @Override
    public void clear(String lob) {
//        distributedCache.clearCache(lob, null, CACHE_DOMAIN);
        profilesCache.clear();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        setInstance(this);
    }

    private static synchronized void setInstance(ProfileRegistry r){
        INSTANCE = r;
    }

    static <E> List<E> nativeQuery(EntityManager em, org.jooq.Query query, Class<E> type) {
        Query result = em.createNativeQuery(query.getSQL(), type);
        int i = 1;
        for (Param<?> param : query.getParams().values())
            if (!param.isInline())
                result.setParameter(i++, convertToDatabaseType(param));
        return result.getResultList();
    }
    static <T> Object convertToDatabaseType(Param<T> param) {
        return param.getBinding().converter().to(param.getValue());
    }


//    public boolean isProfileExists(String lob){
//        JdbcTemplate jdbcTemplate = JdbcUtils.createJdbcTemplate(DatabaseProfileRegistry.getDefaultDs());
//        List<String> names = jdbcTemplate.query("select name from profile where lob='"+lob+"' and type='database'", (rs, rowNum) -> rs.getString("name"));
//        return !names.isEmpty();
//    }

}
