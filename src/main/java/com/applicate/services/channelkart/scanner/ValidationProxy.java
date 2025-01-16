package com.applicate.services.channelkart.scanner;

import com.applicate.services.channelkart.security.SecurityContextUtils;
import com.applicate.services.channelkart.validations.AbstractRule;
import com.applicate.services.channelkart.validations.RuleInfo;
import javassist.util.proxy.ProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/***
 * Dhaneesh
 */
public class ValidationProxy {

  private static final Logger log = LoggerFactory.getLogger(ValidationProxy.class);

  private final ResourcesLoader resourcesLoader;
  ProxyFactory factory = new ProxyFactory();
  private static final Map<String,AbstractRule<Object>> classMap = new ConcurrentHashMap<>();

  public ValidationProxy(ResourcesLoader resourcesLoader) {
    factory.setSuperclass(AbstractRule.class);
    this.resourcesLoader = resourcesLoader;
  }
  @SuppressWarnings("unchecked")
  public AbstractRule<Object> createValidationRuleProxy() {
    try {
      return (AbstractRule<Object>) factory.create(new Class<?>[0], new Object[]{}, (Object self, Method thisMethod, Method proceed, Object[] args)->{
        RuleInfo rinfo = (RuleInfo) args[0];
        AbstractRule<Object> ar  = classMap.get(SecurityContextUtils.getLob() + ":" + rinfo.getImplementation());
        if(ar==null) {
          ClassLoader cl = resourcesLoader.getLoaderMap()
              .get(SecurityContextUtils.getLob() + ":" + rinfo.getImplementation())
              .getClassLoader();
          ar = (AbstractRule<Object>) cl.loadClass(rinfo.getImplementation()).getDeclaredConstructor().newInstance();
          classMap.put(SecurityContextUtils.getLob() + ":" + rinfo.getImplementation(),ar);
        }
        return ar.apply(args[1]);
      });
    }  catch (Exception e) {
      log.error("validation proxy creation failed",e);
    }
    return null;
  }

}
