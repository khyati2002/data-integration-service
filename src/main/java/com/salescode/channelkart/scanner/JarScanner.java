package com.salescode.channelkart.scanner;

import com.salescode.channelkart.enrichments.AbstractEnrichment;
import com.salescode.channelkart.scanner.ResourcesLoader.ResourcesInfo;
import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.channelkart.transformers.AbstractTransformer;
import com.salescode.channelkart.validations.AbstractRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/***
 * Dhaneesh
 */
public class JarScanner {
    private static  Logger logger = LoggerFactory.getLogger(JarScanner.class);
    public JarScanner() {
    }
    public HashMap<String, ResourcesInfo> scan(String jarDirectory) throws Exception {
        logger.debug("jar directory:{}" , jarDirectory);
        HashMap<String, ResourcesInfo> loaderMap;
        try (URLClassLoader cl = new URLClassLoader(new URL[]{new URL(jarDirectory)}, this.getClass()
                .getClassLoader())) {
            List<String> classes = findClassesInJar(jarDirectory);
            loaderMap = new HashMap<>();
            classes.forEach(c -> {
                try {
                    String className = (c.split("[.]")[0]).replaceAll("[/]", ".");
                    logger.debug("Scanner identified :{}" , className);
                    Class claz = cl.loadClass(className);
                    String type = getType(claz);
                    ResourcesInfo resourcesInfo = new ResourcesInfo();
                    resourcesInfo.setClassLoader(cl);
                    resourcesInfo.setImplementation(className);
                    resourcesInfo.setType(type);
                    resourcesInfo.setLob(SecurityContextUtils.getLob());
                    loaderMap.put(SecurityContextUtils.getLob() + ":" + className, resourcesInfo);
                } catch (ClassNotFoundException e) {
                    logger.error("Could not find class for resource loader", e);
                }
            });
        }
        return loaderMap;
    }

    private String getType(Class rc){
        if(AbstractRule.class.isAssignableFrom(rc)){
            return "validation";
        }else if(AbstractTransformer.class.isAssignableFrom(rc)){
            return "transformer";
        }else if(AbstractEnrichment.class.isAssignableFrom(rc)){
            return "enrichment";
        }else {
            return null;
        }
    }

    private List<String> findClassesInJar(String jarFilename) throws Exception {
        List<String> classFiles = new ArrayList<String>();
        URL url = new URL(jarFilename);
        URL jarURL = new URL("jar:" + url.toExternalForm() + "!/");
        JarURLConnection jarConn = (JarURLConnection) jarURL.openConnection();
        JarFile jarFile = jarConn.getJarFile();
        final Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();
            final String entryName = entry.getName();
            if (entryName.endsWith(".class")) {
                classFiles.add(entryName);
            }
        }
        return classFiles;
    }
}