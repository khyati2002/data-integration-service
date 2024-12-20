package com.salescode.channelkart.security;


import com.salescode.DataIntegrationApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityContextUtils {

    private static final Logger log = LoggerFactory.getLogger(SecurityContextUtils.class);


    private SecurityContextUtils() {
        throw new IllegalStateException("Utility Class");
    }


    public static String getLob() {
//        String tempLOB = getTempLOB();
//        if (StringUtils.isNotEmpty(tempLOB)) {
//            return tempLOB;
//        }
//        JwtUser user = getJWTUser();
//        String lob = (user == null) ? tempLOB : user.getLob();
//        if (lob == null) {
//            lob = getLobFromRequest();
//        }
//        if (lob == null) {
//            lob = AbstractDataSourceConstants.DEFAULT;
//        }
//        return lob;
        return DataIntegrationApplication.getLob();
    }

    public static String getPrincipal(){
        return "integration_user";
    }


}
