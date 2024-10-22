/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The class StringUtils.
 *
 * @author Manish Srivastava
 * @since  May 2020
 */
public class StringUtils {

    private static final Logger log = LoggerFactory.getLogger(StringUtils.class);

    private static String regexString = "\\{\\{[^}]*}}";

    /*
     * Private constructor to solve Sonar Issue
     */
    private StringUtils(){}

    private static String getReplaceValueFromKeys(String[] keyArr, JSONObject dataObj){
        String value = "";
        for (String keystr : keyArr) {
            if(keystr.contains(".")) {
                String qkey= "/"+keystr.replace(".","/");
                value = String.valueOf(dataObj.optQuery(qkey));
            }else {
                value = dataObj.optString(keystr);
            }
            if(!keystr.contains("'") && "".equals(value)){
                continue;
            }else{
                if(!"".equals(value)){
                    break;
                }
                if(keystr.contains("'")){
                    value = keystr.replaceAll(Pattern.quote("'"), "");
                    break;
                }
            }
        }
        return value;
    }

    /**
     * Replace dynamic values.
     *
     * @param str the str
     * @param dataObj the data obj
     * @return the string
     * @throws JSONException the JSON exception
     */
    public static String replaceDynamicValues(String str,JSONObject dataObj) throws JSONException{
        Pattern pattern = Pattern.compile(regexString, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            String matcherStr = matcher.group();
            String key = matcherStr.replaceAll(Pattern.quote("{{"), "");
            key = key.replaceAll(Pattern.quote("}}"), "");
            String[] keyArr = key.split(":");
            String value = getReplaceValueFromKeys(keyArr, dataObj);
            str = str.replaceAll(Pattern.quote(matcherStr), value);
        }
        return str;
    }
    public static String replaceDynamicValues(String str,ObjectNode dataObj) throws JSONException{
        try {
            ObjectMapper mapper = new ObjectMapper();
            return replaceDynamicValues(str,new JSONObject(mapper.writeValueAsString(dataObj)));
        } catch (Exception e) {
            log.error("stacktrace", e);
            return null;
        }
    }

    private static String getValueFromKeys(String[] keyArr, JSONObject dataObj){
        String value = null;
        for (String keystr : keyArr) {
            value = getValue(dataObj,keystr);
            if(!keystr.contains("'") && "".equals(value)){
                continue;
            }else{
                if(!"".equals(value)){
                    break;
                }
                if(keystr.contains("'")){
                    value = keystr.replaceAll(Pattern.quote("'"), "");
                    break;
                }
            }
        }
        return value;
    }

    /**
     * Replace dynamic values.
     *
     * @param str the str
     * @param dataObj the data obj
     * @param patternStr the pattern str
     * @param startWith the start with
     * @param endWith the end with
     * @param replaceEmptyStringIfValueNotExist the replace empty string if value not exist
     * @return the string
     * @throws JSONException the JSON exception
     */
    public static String replaceDynamicValues(String str,JSONObject dataObj,String patternStr,String startWith,String endWith,boolean replaceEmptyStringIfValueNotExist) throws JSONException{
        Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            String matcherStr = matcher.group();
            String key = matcherStr.replaceAll(Pattern.quote(startWith), "");
            key = key.replaceAll(Pattern.quote(endWith), "");
            String[] keyArr = key.split(":");
            String value = getValueFromKeys(keyArr, dataObj);
            if(value==null&&replaceEmptyStringIfValueNotExist){
                value ="";
            }
            if(value !=null){
                str = str.replaceAll(Pattern.quote(matcherStr), Matcher.quoteReplacement(value));
            }
        }
        return str;
    }

    /**
     * Gets the value.
     *
     * @param dataObj the data obj
     * @param key the key
     * @return the value
     * @throws JSONException the JSON exception
     */
    private static String getValue(JSONObject dataObj,String key) throws JSONException {
        if(key.contains("||")){
            String[] strArr = key.split(Pattern.quote("||"));
            String keyValue = "";
            for (String keyStr:strArr) {
                if(dataObj.has(keyStr) && !dataObj.get(keyStr).equals("")){
                    keyValue = dataObj.getString(keyStr);
                    return keyValue;
                }else{
                    keyValue = keyStr;
                }
            }
            return keyValue;
        }else{
            return  dataObj.optString(key,null);
        }
    }

    /**
     * Removes the last word.
     *
     * @param str the str
     * @param lastWord the last word
     * @return the string
     */
    public static String removeLastWord(String str , String lastWord){
        str = str.trim();
        String lastword = str.substring(str.lastIndexOf(" ") + 1, str.length());
        if(lastWord.equals(lastword)) {
            return str.substring(0, str.lastIndexOf(lastword));
        }
        return str;
    }

    public static boolean containsWordIgnoreCase(String str, String word) {
        String regex = ".*\\b" + Pattern.quote(word) + "\\b.*";
        Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(str);
        return m.find();
    }


    /**
     * Replace dynamic values of regex at.
     *
     * @param str the str
     * @param dataObj the data obj
     * @param replaceEmptyStringIfValueNotExist the replace empty string if value not exist
     * @return the string
     * @throws JSONException the JSON exception
     */
    public static String replaceDynamicValuesOfRegexAt(String str,JSONObject dataObj,boolean replaceEmptyStringIfValueNotExist) throws JSONException {
        String pattern = "@@[^@]*@@";
        String startWith = "@@";
        String endWith = "@@";
        return replaceDynamicValues(str,dataObj,pattern,startWith,endWith,replaceEmptyStringIfValueNotExist);
    }



    /**
     * Replace dynamic values of regex flower braces.
     *
     * @param str the str
     * @param dataObj the data obj
     * @param replaceEmptyStringIfValueNotExist the replace empty string if value not exist
     * @return the string
     * @throws JSONException the JSON exception
     */
    public static String replaceDynamicValuesOfRegexFlowerBraces(String str,JSONObject dataObj,boolean replaceEmptyStringIfValueNotExist) throws JSONException {
        String pattern = regexString;
        String startWith = "{{";
        String endWith = "}}";
        return replaceDynamicValues(str,dataObj,pattern,startWith,endWith,replaceEmptyStringIfValueNotExist);
    }


    /**
     * Gets the string content.
     *
     * @param inputStream the input stream
     * @return the string content
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static String getStringContent(InputStream inputStream) throws IOException{
        StringBuilder jb = new StringBuilder();
        String line = null;
        BufferedReader reader = null;
        if (inputStream != null) {
            reader = new BufferedReader(new InputStreamReader(inputStream));
            while ((line = reader.readLine()) != null){
                jb.append(line);
            }
            reader.close();
        }
        return jb.toString();
    }


    /**
     * Checks if is object string.
     *
     * @param obj the obj
     * @return true, if is object string
     */
    public static boolean isObjectString(Object obj){
        return (obj instanceof String);
    }

    /**
     * Cast to string.
     *
     * @param data the data
     * @return the string
     * @throws JSONException the JSON exception
     */
    public static String castToString(Object data) throws JSONException {
        if(data!=null){
            return (String)data;
        }else{
            throw new JSONException("Object value should not be NULL");
        }
    }

    /**
     * Checks if is string contains values.
     *
     * @param str the str
     * @param stringArr the string arr
     * @return true, if is string contains values
     */
    public static boolean isStringContainsValues(String str,String[] stringArr){
        boolean result = false;
        for (String strTemp : stringArr){
            if(str.contains(strTemp)){
                return true;
            }
        }
        return result;
    }


    /**
     * Gets the dynamic values keys.
     *
     * @param str the str
     * @return the dynamic values keys
     */
    public static List<String> getDynamicValuesKeys(String str) {
        List<String> keys = new ArrayList<>();
        if(!isEmpty(str)) {
            Pattern pattern = Pattern.compile(regexString, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(str);
            while (matcher.find()) {
                String matcherStr = matcher.group();
                String key = matcherStr.replaceAll(Pattern.quote("{{"), "");
                key = key.replaceAll(Pattern.quote("}}"), "");
                keys.add(key);
            }
        }
        return keys;
    }


    /**
     * Checks if is equal.
     *
     * @param firstValue the first value
     * @param secondValue the second value
     * @param ignoreCase the ignore case
     * @return true, if is equal
     */
    public static boolean isEqual(String firstValue, String secondValue, boolean ignoreCase) {
        if(ignoreCase) {
            return firstValue.equalsIgnoreCase(secondValue);
        }
        return firstValue.equals(secondValue);
    }


    /**
     * Checks if is empty.
     *
     * @param value the value
     * @return true, if is empty
     */
    public static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    /**
     * Contains empty.
     *
     * @param values the values
     * @return true, if successful
     */
    public static boolean containsEmpty(String... values) {
        for(String value : values) {
            if(isEmpty(value)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Checks for null or empty values.
     *
     * @param values the values
     * @return true, if successful
     */
    public static boolean hasNullOrEmptyValues(String... values) {
        for (String value : values) {
            if(value == null || value.isEmpty()) {
                return true;
            }
        }
        return false;
    }


    /**
     * Checks if is object empty.
     *
     * @param object the object
     * @return true, if is object empty
     */
    public static boolean isObjectEmpty(Object object) {
        return object == null || object.toString().isEmpty();
    }


    /**
     * Checks if is not empty.
     *
     * @param value the value
     * @return true, if is not empty
     */
    public static boolean isNotEmpty(String value) {
        return !isEmpty(value);
    }


    public static boolean isNotEmptyAndNA(String value) {
        return isNotEmpty(value) && !value.equalsIgnoreCase("NA");
    }
    /**
     * Gets the curly brackets matcher key.
     *
     * @param value the value
     * @return the curly brackets matcher key
     */
    public static String getCurlyBracketsMatcherKey(String value) {
        Pattern pattern = Pattern.compile("\\{\\{[^}]*}}", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(value);
        if (matcher.find()) {
            String matcherStr = matcher.group();
            String key = matcherStr.replaceAll(Pattern.quote("{{"), "");
            key = key.replaceAll(Pattern.quote("}}"), "");
            return key;
        }
        return null;
    }


    /**
     * Starts with ignore case.
     *
     * @param str the str
     * @param prefix the prefix
     * @return true, if successful
     */
    public static boolean startsWithIgnoreCase(String str, String prefix) {
        return str.regionMatches(true, 0, prefix, 0, prefix.length());
    }


    /**
     * Ends with ignore case.
     *
     * @param str the str
     * @param suffix the suffix
     * @return true, if successful
     */
    public static boolean endsWithIgnoreCase(String str, String suffix) {
        int suffixLength = suffix.length();
        return str.regionMatches(true, str.length() - suffixLength, suffix, 0, suffixLength);
    }

    /**
     * Checks if is valid string.
     *
     * @param value the value
     * @return true, if is valid string
     */
    public static boolean isValidString(String value) {
        return !isEmpty(value) && !value.equals("null") && !value.equals("\"\"") && !value.equalsIgnoreCase("undefined");
    }

    /**
     * Gets the last character.
     *
     * @param value the value
     * @return the last character
     */
    public static String getLastCharacter(String value) {
        return value.isEmpty() ? "" : value.substring(value.length() - 1);
    }

    /**
     * Gets the first character.
     *
     * @param value the value
     * @return the first character
     */
    public static String getFirstCharacter(String value) {
        return value.isEmpty() ? "" : value.substring(0, 1);
    }

    /**
     * Contains all.
     *
     * @param value the value
     * @param matchingStrings the matching strings
     * @return true, if successful
     */
    public static boolean containsAll(String value, String... matchingStrings) {
        for(String matchingString : matchingStrings) {
            if (!value.contains(matchingString)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Starts with vowel.
     *
     * @param value the value
     * @return true, if successful
     */
    public static boolean startsWithVowel(String value) {
        final String vowel = "aeiou";
        return !isEmpty(value) && org.apache.commons.lang.StringUtils.contains(vowel, value.charAt(0));
    }

    /**
     * Prefix A or an.
     *
     * @param value the value
     * @return the string
     */
    public static String prefixAOrAn(String value) {
        return startsWithVowel(value) ? "an " + value : "a " + value;
    }

    /**
     * Trim last character.
     *
     * @param builder the builder
     * @param lastCharacter the last character
     * @return the string builder
     */
    public static StringBuilder trimLastCharacter(StringBuilder builder, String lastCharacter) {
        for(int lastIndex = builder.length() - 1; lastIndex >= 0; lastIndex --) {
            String character = builder.substring(lastIndex, lastIndex + 1);
            if(character.equals(" ")) {
                continue;
            }
            if (character.equals(lastCharacter)) {
                builder.setLength(lastIndex);
            }
            break;
        }
        return builder;
    }

    /**
     * Trim last character.
     *
     * @param string the string
     * @param lastCharacter the last character
     * @return the string
     */
    public static String trimLastCharacter(String string, String lastCharacter){
        String stripedString = org.apache.commons.lang.StringUtils.stripEnd(string.trim(), lastCharacter);
        if(!StringUtils.isEmpty(stripedString)){
            return stripedString;
        }
        return string;
    }

    /**
     * Extract string from brackets.
     *
     * @param textToExtract the text to extract
     * @return the string
     */
    public static String extractStringFromBrackets(String textToExtract) {
        int startingPoint = textToExtract.indexOf("(") + 1;
        int endingPoint = textToExtract.lastIndexOf(")");
        if (startingPoint > 0 && endingPoint > 0 && startingPoint <= endingPoint) {
            return textToExtract.substring(startingPoint, endingPoint);
        }
        return null;
    }

    /**
     * Gets the enclosed values.
     *
     * @param str the str
     * @param regex the regex
     * @param stringToReplace the string to replace
     * @return the enclosed values
     */
    public static Set<String> getEnclosedValues(String str, String regex, String stringToReplace) {
        Set<String> values = new HashSet<>();
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            String matchingString = matcher.group().replaceAll(stringToReplace, "");
            values.add(matchingString);
        }
        return values;
    }

    /**
     * Require non empty.
     *
     * @param valueToCheck the value to check
     * @param message the message
     * @return the string
     */
    public static String requireNonEmpty(String valueToCheck, String message) {
        if (valueToCheck == null || valueToCheck.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return valueToCheck;
    }

    /**
     * Are not empty and equal.
     *
     * @param arg1 the arg 1
     * @param arg2 the arg 2
     * @return true, if successful
     */
    public static boolean areNotEmptyAndEqual(String arg1, String arg2) {
        return !(arg1 == null || arg2 == null) && arg1.equals(arg2);
    }

    /**
     * Checks if is not null.
     *
     * @param data the data
     * @return true, if is not null
     */
    public static boolean isNotNull(Object data){
        return !(data == null || "null".equalsIgnoreCase(data.toString()));
    }

    public static boolean isNotBlank(String input) {
        return input != null && !input.isBlank()	;
    }

    /**
     * Replace characters.
     *
     * @param data the data
     * @param replaceCharacter the replace character
     * @param replaceWith the replace with
     * @return the string
     */
    public static String replaceCharacters(String data,String[] replaceCharacter, String replaceWith){
        if(isValidString(data)){
            for(int i=0;i<replaceCharacter.length;i++){
                data  = data.replace(replaceCharacter[i], replaceWith);
            }
        }
        return data;
    }


    /**
     * Version compare.
     *
     * @param str1 the str 1
     * @param str2 the str 2
     * @return the int
     */
    public static int versionCompare(String str1, String str2) {
        String[] vals1 = str1.split("\\.");
        String[] vals2 = str2.split("\\.");
        int i = 0;
        // set index to first non-equal ordinal or length of shortest version string
        while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
            i++;
        }
        // compare first non-equal ordinal number
        if (i < vals1.length && i < vals2.length) {
            int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
            return Integer.signum(diff);
        }
        // the strings are equal or one string is a substring of the other
        // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
        return Integer.signum(vals1.length - vals2.length);
    }

    /**
     * Checks if is version equal.
     *
     * @param ver1 the ver 1
     * @param ver2 the ver 2
     * @return true, if is version equal
     */
    public static boolean isVersionEqual(String ver1, String ver2){
        return versionCompare(ver1, ver2) == 0;
    }

    /**
     * Checks if is version lesser.
     *
     * @param ver1 the ver 1
     * @param ver2 the ver 2
     * @return true, if is version lesser
     */
    public static boolean isVersionLesser(String ver1, String ver2){
        return versionCompare(ver1, ver2) == -1;
    }

    /**
     * Checks if is version greater.
     *
     * @param ver1 the ver 1
     * @param ver2 the ver 2
     * @return true, if is version greater
     */
    public static boolean isVersionGreater(String ver1, String ver2){
        return versionCompare(ver1, ver2) == 1;
    }

    /**
     * Contains ignore case.
     *
     * @param str the str
     * @param searchStr the search str
     * @return true, if successful
     */
    public static boolean containsIgnoreCase(String str, String searchStr) {
        if (str == null || searchStr == null) {
            return false;
        }
        int len = searchStr.length();
        int max = str.length() - len;
        for (int i = 0; i <= max; i++) {
            if (str.regionMatches(true, i, searchStr, 0, len)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes the empty value from string.
     *
     * @param value the value
     * @param symbol the symbol
     * @return the string
     */
    public static String removeEmptyValueFromString(String value,String symbol){
        String str = "";
        for (String val : value.split(symbol)) {
            if(!val.isEmpty())
                str = str + val + symbol;
        }
        if(str.length()>0)
            return str.substring(0, str.length()-1);
        return str;
    }

    /**
     * Replace characters.
     *
     * @param data the data
     * @param regex the regex
     * @param replaceWith the replace with
     * @return the string
     */
    public static String replaceCharacters(String data,String regex, String replaceWith){
        if(isValidString(data)){
            return data.replaceAll(regex,replaceWith);
        }
        return data;
    }

    public static String[] toArray(String commaSeparatedValues) {
        return Arrays.stream(commaSeparatedValues.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .toArray(String[]::new);
    }

    public static List<String> toList(String commaSeparatedValues) {
        return Arrays.asList(toArray(commaSeparatedValues));
    }

    public static String format(final String str,Object... values) throws JSONException{
        synchronized(str.intern()) {
            Pattern pattern = Pattern.compile("\\{[^}]*}", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(str);
            int index= 0;
            String output = str;
            while (matcher.find()) {
                StringBuilder stringBuilder= new StringBuilder(output);
                String value = "";
                if(values.length > index) {
                    value= String.valueOf(values[index]);
                }else {
                    break;
                }
                stringBuilder.replace(matcher.start(), matcher.end(), value);
                output = stringBuilder.toString();
                matcher = pattern.matcher(output);
                index++;
            }
            return output;
        }
    }

    /**
     * Checks if is null or blank.
     *
     * @param value the value
     * @return true, if is null or blank
     */
    public static boolean isNullOrBlank(Object value) {
        return (value == null) || (String.valueOf(value).isBlank());
    }

    public static String getDefaultEmptyValue(Object value) {
        if(value instanceof JsonNode)
            return ((JsonNode) value).asText();
        return value == null ? "" : value.toString();
    }

    public static boolean isContainWord(String src, String word) {
        String pattern = "\\b"+word+"\\b";
        Pattern p=Pattern.compile(pattern);
        Matcher m=p.matcher(src);
        return m.find();
    }

    public static boolean hasContainWords(String src, List<String> words) {
        return words.stream()
                .anyMatch(word -> isContainWord(src, word));
    }



    public static boolean hasMatches(String src, String... matches) {
        if (matches == null) {
            matches = new String[]{};
        }
        for (String value : matches) {
            if (Objects.equals(value, src)) {
                return true;
            }
        }
        return false;
    }

    public static boolean anyMatch(String key, Collection<String> options) {
        return options.contains(key);
    }

    public static List<String> splitEqually(String text, int size) {
        List<String> result = new ArrayList<>((text.length() + size - 1) / size);
        for (int start = 0; start < text.length(); start += size) {
            result.add(text.substring(start, Math.min(text.length(), start + size)));
        }
        return result;
    }

    /**
     *  make input sql compatible
     * @param input string to change
     * @return
     */
    public static String escapeSql(String input) {
        input = input.replace("\\", "\\\\");
        return input.replace("'", "''");
    }

    public static String encloseQuotes(String name) {
        return "'" + name + "'";
    }

    public static void runIfNotEmpty(String key, Consumer<String> consumer) {
        if (StringUtils.isNotEmpty(key)) {
            consumer.accept(key);
        }
    }

    public static String safeJoin(String delimiter, String... args) {
        String[] safeArgs= Arrays.stream(args)
                .map(item -> item == null ? "null" : item)
                .toArray(String[]::new);
        return String.join(delimiter, safeArgs);
    }

    public static String removeSpecialAndLower(String input){
        return input.replaceAll("[^\\w,]","").toLowerCase();
    }
}
