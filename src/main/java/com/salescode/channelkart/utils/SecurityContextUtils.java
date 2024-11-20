//package com.salescode.channelkart.utils;
//
//import com.salescode.channelkart.models.diff.UserContext;
//import com.salescode.dataintegration.etl.cdm.abstractdatasource.AbstractDataSourceConstants;
//
//import java.util.Optional;
//
//public class SecurityContextUtils {
//
//    private static final ThreadLocal<UserContext> userContextHolder = new ThreadLocal<>();
//
//    private SecurityContextUtils() {
//        throw new IllegalStateException("Utility Class");
//    }
//
////    public static String getPrincipal() {
////        JwtUser user = getJWTUser();
////        return user != null ? user.getUsername() : getTempUserName();
////    }
//
//    public static String getLob() {
//        String tempLOB = getTempLOB();
//        if (StringUtils.isNotEmpty(tempLOB)) {
//            return tempLOB;
//        }
////        JwtUser user = getJWTUser();
////        String lob = (user == null) ? tempLOB : user.getLob();
////        if (lob == null) {
////            lob = getLobFromRequest();
////        }
////        if (lob == null) {
////            lob = AbstractDataSourceConstants.DEFAULT;
////        }
//     //   return lob;
//        return AbstractDataSourceConstants.DEFAULT;
//    }
//
//    public static String getTempLOB() {
//        return getUserContext()
//                .map(UserContext::getLob)
//                .orElse(null);
//    }
//
//    public static Optional<UserContext> getUserContext() {
//        return Optional.ofNullable(userContextHolder.get());
//    }
//
//
//}
//
