package com.applicate.services.channelkart.models.enums;

/**
 * GRNStatus ENUM
 *
 * Enum defining possible statuses for a GRN.
 *
 * Values :
 *
 * ACCEPTED: Indicates that the GRN has been accepted.
 * REJECTED: Indicates that the GRN has been rejected.
 * PARTIALLY_REJECTED: Indicates that the GRN has been partially rejected.
 * OPEN: Indicates that the GRN is currently open.
 *
 * @author abhay.thakur@salescode.ai
 * @since 26/7/24
 */
public enum GRNStatus {
    ACCEPTED,
    REJECTED,
    PARTIALLY_REJECTED,
    OPEN;

    /**
     * validate string if it is GRNStatus or not
     *
     * @param grnStatus
     * @return boolean
     */
    public static boolean isValid(String grnStatus) {
        for (GRNStatus s : values()) {
            if (s.name().equalsIgnoreCase(grnStatus)) {
                return true;
            }
        }
        return false;
    }

    /**
     * return GRNStatus enum after comparing string, if not matched ,
     * by default returns OPEN
     *
     * @param grnStatus
     * @return GRNStatus
     */
    public static GRNStatus getStatus(String grnStatus) {
        for (GRNStatus s : GRNStatus.values()) {
            if (s.name().equalsIgnoreCase(grnStatus)) {
                return s;
            }
        }
        return GRNStatus.OPEN;
    }
}
