package com.applicate.services.channelkart.profiles;

import com.applicate.services.channelkart.abstractdatasource.DatabaseProfileRegistry;
import com.applicate.services.channelkart.cache.AllLOBRouter;
import com.applicate.services.channelkart.cache.AppCacheManager;
import com.applicate.services.channelkart.cache.DistributedCache;
import com.applicate.services.channelkart.models.Profile;
import com.applicate.services.channelkart.registry.AbstractRegistry;
import com.applicate.services.channelkart.utils.JdbcUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class ProfileRegistry extends AbstractRegistry<Profile> implements InitializingBean {

	//public static final ProfileRegistry INSTANCE = new ProfileRegistry();
	public static ProfileRegistry INSTANCE;

	private static final List<Profile> EMPTY_PROFILES=Collections
			.unmodifiableList(new ArrayList<>());
	private DistributedCache distributedCache =null;

	private static final String CACHE_DOMAIN = "profile";

	public ProfileRegistry(DistributedCache distributedCache) {
	/*	try {
			distributedCache = SpringContext.getBean(DistributedCache.class);
		}catch (Exception e) {
			log.error("stacktrace", e);
		}
	*/
		this.distributedCache= distributedCache;
	}

	public DistributedCache getDistributedCache() {
		return distributedCache;
	}

	public void setDistributedCache(DistributedCache distributedCache) {
		this.distributedCache = distributedCache;
	}

	@Override
	public void add(Profile profile) {
		synchronized (profile.getLob().intern()) {
			List<Profile> rules = (List<Profile>) distributedCache.get(profile.getLob(), null, CACHE_DOMAIN, false);
			if (rules == null) {
				rules = new ArrayList<Profile>();
			}
			rules.add(profile);
			clear(profile.getLob());
			distributedCache.put(profile.getLob(), null, CACHE_DOMAIN, rules, false);
		}
	}

	@Override
	public List<Profile> get(String lob) {
		List<Profile> profiles = distributedCache.withCache(lob, null, CACHE_DOMAIN,
				(s)->AllLOBRouter.loadAll(Profile.class,(k) -> k.equalsIgnoreCase(lob)).stream().map(e->(Profile)e).collect(Collectors.toList()));
		return lob!=null && profiles != null? profiles :EMPTY_PROFILES;
	}

	@Override
	public List<Profile> get(String lob, String type) {
		return get(lob,(rule)->rule.getType().equalsIgnoreCase(type));

	}

	public List<Profile> get(String lob, String type, String  name) {
		return AppCacheManager.getInstance().withCache(lob+":"+type,name,(s)-> get(lob,rule->(rule.getType().equalsIgnoreCase(type) && rule.getName().equals(name))));
	}

	@Override
	public void addAll(String lob,List<Profile> profile) {
		synchronized (lob.intern()) {
			clear(lob);
			distributedCache.put(lob, null, CACHE_DOMAIN, profile, false);
		}
	}

	@Override
	public void loadAll(Predicate<String> predicate,boolean overrideOld) {
		AllLOBRouter.loadAll(Profile.class,predicate).stream().map(e->(Profile)e).collect(Collectors.groupingBy(a->a.getLob())).
				entrySet().stream().forEach(e->{
					if(distributedCache.get(e.getKey(), null, CACHE_DOMAIN, false)==null || overrideOld)
						addAll(e.getKey(),e.getValue());
				});
	}

	@Override
	public void clear(String lob) {
		distributedCache.clearCache(lob, null, CACHE_DOMAIN);

	}

	@Override
	public void afterPropertiesSet() throws Exception {
		setInstance(this);
	}

	private static synchronized void setInstance(ProfileRegistry r){
		INSTANCE = r;
	}


	public boolean isProfileExists(String lob){
		JdbcTemplate jdbcTemplate = JdbcUtils.createJdbcTemplate(DatabaseProfileRegistry.getDefaultDs());
		List<String> names = jdbcTemplate.query("select name from profile where lob='"+lob+"' and type='database'", (rs, rowNum) -> rs.getString("name"));
		return !names.isEmpty();
	}

}
