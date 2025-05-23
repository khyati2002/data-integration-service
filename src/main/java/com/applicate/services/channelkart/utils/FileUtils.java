package com.applicate.services.channelkart.utils;

import org.apache.commons.io.FilenameUtils;

public class FileUtils {

    private FileUtils() {
    }

    public static String getSimpleFileNameWithExtenstion(String filepath) {
        if (filepath == null) {
            return null;
        } else {
            synchronized(filepath.intern()) {
                String basename = FilenameUtils.getBaseName(filepath);
                String modbase = StringUtils.replaceCharacters(basename, "[^A-Za-z0-9_\\/\\-()]", "");
                String ext = FilenameUtils.getExtension(filepath);
                return ext != null && !ext.isEmpty() ? modbase + "." + ext : modbase;
            }
        }
    }
}
