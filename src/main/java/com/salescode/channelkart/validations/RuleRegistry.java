package com.salescode.channelkart.validations;

import com.salescode.channelkart.exceptions.CustomRuntimeException;
import com.salescode.channelkart.registry.AbstractRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class RuleRegistry extends AbstractRegistry<RuleInfo> {
    public static final RuleRegistry INSTANCE = new RuleRegistry();
    private static final List<RuleInfo> EMPTY_VALIDATIONS = Collections.unmodifiableList(new ArrayList<>());
    private static final String CACHE_DOMAIN = "validationrule";
//    private final DistributedCache distributedCache = SpringContext.getBean(DistributedCache.class);

    private RuleRegistry() {
    }

//    @Override
//    public void add(RuleInfo ruleInfo) {
//        GlobalLock.withLock(ruleInfo.getLob(),key->{
//            List<RuleInfo> rules = (List<RuleInfo>) distributedCache.get(ruleInfo.getLob(), null, CACHE_DOMAIN, false);
//            if (rules == null) {
//                rules = new ArrayList<>();
//            }
//            rules.add(ruleInfo);
//            clear(ruleInfo.getLob());
//            distributedCache.put(ruleInfo.getLob(), null, CACHE_DOMAIN, rules, false);
//        });
//    }
//
//    @Override
//    public List<RuleInfo> get(String lob) {
//        List<RuleInfo> rules = AppCacheManager
//                .getInstance().withCache(lob,CACHE_DOMAIN,sk-> distributedCache.withCache(lob, null, CACHE_DOMAIN,
//                        s->AllLOBRouter.loadAll(RuleInfo.class,k -> k.equalsIgnoreCase(lob)).stream().map(RuleInfo.class::cast).collect(Collectors.toList())));
//        return lob !=null && rules != null ? rules:EMPTY_VALIDATIONS;
//    }
//
//    @Override
//    public List<RuleInfo> get(String lob, String type) {
//        return get(lob,rule->rule.getType().equalsIgnoreCase(type) && rule.isEnabled());
//    }
//
//    @Override
//    public void addAll(String lob,List<RuleInfo> ruleInfo) {
//        GlobalLock.withLock( lob,key-> {
//            clear(lob);
//            distributedCache.put(lob, null, CACHE_DOMAIN, ruleInfo, false);
//        });
//    }
//
//    @Override
//    public void loadAll(Predicate<String> predicate,boolean overrideOld) {
//        AllLOBRouter.loadAll(RuleInfo.class,predicate).stream().map(RuleInfo.class::cast).collect(Collectors.groupingBy(
//                        CommonDataModel::getLob)).
//                entrySet().stream().forEach(e->{
//                    if(distributedCache.get(e.getKey(), null, CACHE_DOMAIN, false)==null || overrideOld)
//                        addAll(e.getKey(),e.getValue());
//                });
//    }
//
//    @Override
//    public void clear(String lob) {
//        distributedCache.clearCache(lob, null, CACHE_DOMAIN);
//    }


    @Override
    public void add(RuleInfo ruleInfo) {
        throw new CustomRuntimeException("not implemented");
    }

    @Override
    public void addAll(String lob, List<RuleInfo> t) {
        throw new CustomRuntimeException("not implemented");
    }

    @Override
    public void loadAll(Predicate<String> predicate, boolean overrideOld) {
        throw new CustomRuntimeException("not implemented");
    }

    @Override
    public List<RuleInfo> get(String lob) {
        throw new CustomRuntimeException("not implemented");
    }

    @Override
    public List<RuleInfo> get(String lob, String type) {
        throw new CustomRuntimeException("not implemented");
    }

    @Override
    public List<RuleInfo> get(String lob, Predicate<RuleInfo> predicate) {
        throw new CustomRuntimeException("not implemented");
    }

    @Override
    public void clear(String lob) {
        throw new CustomRuntimeException("not implemented");
    }
}
