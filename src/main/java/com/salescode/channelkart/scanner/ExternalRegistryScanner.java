package com.salescode.channelkart.scanner;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.salescode.channelkart.cache.AllLOBRouter;
import com.salescode.channelkart.profiles.ProfileRegistry;
import com.salescode.channelkart.scanner.ResourcesLoader.ResourcesInfo;
import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.channelkart.utils.ReflectionUtils;
import com.salescode.channelkart.validations.AbstractRule;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/***
 * Dhaneesh
 */
public class ExternalRegistryScanner {
    private static Logger logger = LoggerFactory.getLogger(ExternalRegistryScanner.class);
    private  ResourcesLoader resourcesLoader;
    private AbstractRule proxyRule;
    private static final ExternalRegistryScanner INSTANCE=new ExternalRegistryScanner();
    public static ExternalRegistryScanner getInstance(){
        return INSTANCE;
    }
    public void loadAll(Predicate<String> predicate) {
        AllLOBRouter.allLobs().forEach(lob->{
            ProfileRegistry.INSTANCE.get(lob).stream().filter(p->"bundle".equalsIgnoreCase(p.getType())).forEach(p->{
                String path = p.getAttributes().get("artifactURL").asText();
                try{
//                    SecurityContextUtils.switchWithLOB(lob, () -> {
                        ExternalRegistryScanner ers =  ExternalRegistryScanner.getInstance();
                        URL presignedUrl = generatePresignedUrl(path, Duration.ofDays(7).toMillis());
//                        ers.loadBundle(presignedUrl.toString());
//                        return null;
//                    });

                }catch (Exception e){
                    logger.error("Failed to load bundle from path {}", path, e);
                }
            });
        });
    }
    private ExternalRegistryScanner(){
        resourcesLoader= new ResourcesLoader();
        ValidationProxy vp = new ValidationProxy(resourcesLoader);
        proxyRule = vp.createValidationRuleProxy();
    }
    public void loadBundle(String name){
        resourcesLoader.load(name);
    }
    public AbstractRule getValidationProxyRule(){
        return proxyRule;
    }

    public boolean isProxyRule(String name) {
        return isExternal(name);
    }

    @SuppressWarnings("unchecked")
    public <T> T createObject(String name) {
        return (T) getResource(name)
                .map(resource -> {
                    ClassLoader loader = resource.getLoader();
                    return ReflectionUtils.createInstance(name, loader);
                }).orElseThrow(() -> new ResourceNotFoundException("Could not find external implementation with name:" + name));
    }

    public boolean isExternal(String name){
        return resourcesLoader.getLoaderMap().containsKey(SecurityContextUtils.getLob()+":"+name);
    }

    public List<BundleResource> getResources(){
        List<BundleResource> brs = new ArrayList<>();
         resourcesLoader.getLoaderMap().forEach((k,v)->{
             if(SecurityContextUtils.getLob()!=null){
                 if(k.startsWith(SecurityContextUtils.getLob()+":")){
                     brs.add(toBR(v));
                 }
             }else{
                 brs.add(toBR(v));
             }
         });
         return brs;
    }

    public Optional<BundleResource> getResource(String name) {
        return getResources().stream().filter(item -> item.getImplementation().equals(name)).findFirst();
    }


    private BundleResource toBR(ResourcesInfo rinf){
        BundleResource br = new BundleResource();
        br.setLoader(rinf.getClassLoader());
        br.setClazz(rinf.getClazz());
        br.setImplementation(rinf.getImplementation());
        br.setType(rinf.getType());
        br.setLob(rinf.getLob());
        return br;
    }


    public static URL generatePresignedUrl(String path) {
        return generatePresignedUrl(path, Duration.ofMinutes(10).toMillis());
    }

    public static URL generatePresignedUrl(String path,long expiration) {
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withRegion("ap-south-1")
                .build();
        AmazonS3URI s3URI = new AmazonS3URI(path);
        long expirationTime = System.currentTimeMillis()+expiration;
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(s3URI.getBucket(), s3URI.getKey())
                .withMethod(com.amazonaws.HttpMethod.GET)
                .withExpiration(new Date(expirationTime));

        return s3Client.generatePresignedUrl(request);
    }
}