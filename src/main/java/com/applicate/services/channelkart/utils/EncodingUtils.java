package com.applicate.services.channelkart.utils;

import com.applicate.services.channelkart.exceptions.CustomRuntimeException;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class EncodingUtils {
    public static String getMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            StringBuilder hashtext = new StringBuilder(no.toString(16));
            while (hashtext.length() < 32) {
                hashtext.append("0" + hashtext);
            }
            return hashtext.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new CustomRuntimeException(e);
        }
    }
}