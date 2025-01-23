package com.applicate.services.channelkart.utils;

import com.applicate.services.channelkart.abstractdatasource.AbstractDataSourceConstants;

public class DataSourceUtils {

    public static boolean isDefaultDataSource(String lob) {
        return (lob == null || lob.equals(AbstractDataSourceConstants.DEFAULT));
    }
}
