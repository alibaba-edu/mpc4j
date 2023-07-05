package edu.alibaba.mpc4j.common.tool.utils;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Properties utility tool.
 *
 * @author Weiran Liu
 * @date 2022/8/28
 */
public class PropertiesUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesUtils.class);
    /**
     * log4j.properties name
     */
    private static final String LOG4J_PROPERTIES_NAME = "log4j.properties";

    private PropertiesUtils() {
        // empty
    }

    /**
     * load log4j properties.
     */
    public static void loadLog4jProperties() {
        File fileObject = new File(LOG4J_PROPERTIES_NAME);
        try {
            Properties log4jProperties = new Properties();
            log4jProperties.load(new FileInputStream("log4j.properties"));
            PropertyConfigurator.configure(log4jProperties);
            LOGGER.info("Successfully load file {} from the path: {}", LOG4J_PROPERTIES_NAME, fileObject.getAbsolutePath());
        } catch (IOException e) {
            Properties properties = new Properties();
            // log4j.rootLogger=INFO,consoleAppender
            properties.setProperty("log4j.rootLogger", "INFO,consoleAppender");
            // log4j.appender.consoleAppender=org.apache.log4j.ConsoleAppender
            properties.setProperty("log4j.appender.consoleAppender", "org.apache.log4j.ConsoleAppender");
            // log4j.appender.consoleAppender.layout=org.apache.log4j.PatternLayout
            properties.setProperty("log4j.appender.consoleAppender.layout", "org.apache.log4j.PatternLayout");
            // log4j.appender.consoleAppender.layout.ConversionPattern=%d [%t] %-5p %c - %m%n
            properties.setProperty("log4j.appender.consoleAppender.layout.ConversionPattern", "%d [%t] %-5p %c - %m%n");
            PropertyConfigurator.configure(properties);
            LOGGER.info("Cannot find file {} from the path: {} (for customized log4j setting), use default configuration.",
                LOG4J_PROPERTIES_NAME, fileObject.getAbsolutePath());
        }
    }

    /**
     * Gets the properties from a file.
     *
     * @param file the file path.
     * @return the properties.
     */
    public static Properties loadProperties(String file) {
        try (InputStream input = new FileInputStream(file)) {
            Properties properties = new Properties();
            // load a properties file
            properties.load(input);
            return properties;
        } catch (IOException e) {
            File fileObject = new File(file);
            throw new IllegalArgumentException("Failed to load config file: " + fileObject.getAbsolutePath());
        }
    }

    /**
     * Checks if the given keyword is set in the properties.
     *
     * @param properties the properties.
     * @param keyword    the keyword.
     * @return if the given keyword is set in the properties.
     */
    public static boolean containsKeyword(Properties properties, String keyword) {
        String readString = properties.getProperty(keyword);
        return !Objects.isNull(readString);
    }

    /**
     * Reads the string from the properties.
     *
     * @param properties the properties.
     * @param keyword    the keyword.
     * @return the string from the properties.
     */
    public static String readString(Properties properties, String keyword) {
        String readString = Preconditions.checkNotNull(
            properties.getProperty(keyword), "Please set " + keyword
        ).trim();
        LOGGER.info("{} is set, value: {}", keyword, readString);
        return readString;
    }

    /**
     * Reads the string from the properties. If the keyword is not set, return the default value.
     *
     * @param properties   the properties.
     * @param keyword      the keyword.
     * @param defaultValue the default value.
     * @return the string from the properties. If the keyword is not set, return the default value.
     */
    public static String readString(Properties properties, String keyword, String defaultValue) {
        String readString = properties.getProperty(keyword);
        if (readString == null) {
            LOGGER.info("{} is not set (choose default value): {}", keyword, defaultValue);
            return defaultValue;
        } else if (readString.equals(defaultValue)) {
            LOGGER.info("{} is set (equal to the default value): {}", keyword, defaultValue);
            return readString;
        } else {
            LOGGER.info("{} is set (use the set value): {}", keyword, readString);
            return readString;
        }
    }

    /**
     * Reads a String array, each of which is trimmed (i.e., with any leading and trailing whitespace removed).
     *
     * @param properties the properties.
     * @param keyword    the keyword.
     * @return the trimmed String array.
     */
    public static String[] readTrimStringArray(Properties properties, String keyword) {
        String stringArrayString = readString(properties, keyword);
        return Arrays.stream(stringArrayString.split(","))
            .map(String::trim)
            .toArray(String[]::new);
    }

    /**
     * Reads a String array, each of which is trimmed (i.e., with any leading and trailing whitespace removed).
     * If the keyword is not set, return String[0].
     *
     * @param properties the properties.
     * @param keyword    the keyword.
     * @return the trimmed String array. If the keyword is not set, return String[0].
     */
    public static String[] readTrimStringArrayWithDefault(Properties properties, String keyword) {
        String stringArrayString = readString(properties, keyword, "");
        if ("".equals(stringArrayString)) {
            return new String[0];
        } else {
            return Arrays.stream(stringArrayString.split(","))
                .map(String::trim)
                .toArray(String[]::new);
        }
    }

    /**
     * Reads the boolean from the properties.
     *
     * @param properties the properties.
     * @param keyword    the keyword.
     * @return the boolean from the properties.
     */
    public static boolean readBoolean(Properties properties, String keyword) {
        String booleanString = readString(properties, keyword);
        return Boolean.parseBoolean(booleanString);
    }

    /**
     * Reads the boolean from the properties. If the keyword is not set, return the default value.
     *
     * @param properties   the properties.
     * @param keyword      the keyword.
     * @param defaultValue the default value.
     * @return the boolean from the properties. If the keyword is not set, return the default value.
     */
    public static boolean readBoolean(Properties properties, String keyword, boolean defaultValue) {
        String booleanString = readString(properties, keyword, "");
        if ("".equals(booleanString)) {
            return defaultValue;
        } else {
            return Boolean.parseBoolean(booleanString);
        }
    }

    /**
     * Reads the int from the properties.
     *
     * @param properties the properties.
     * @param keyword    the keyword.
     * @return the int from the properties.
     */
    public static int readInt(Properties properties, String keyword) {
        String intString = readString(properties, keyword);
        return Integer.parseInt(intString);
    }

    /**
     * Reads the int from the properties.
     *
     * @param properties   the properties.
     * @param keyword      the keyword.
     * @param defaultValue the default value.
     * @return the int from the properties.
     */
    public static int readIntWithDefault(Properties properties, String keyword, int defaultValue) {
        String intString = readString(properties, keyword, String.valueOf(defaultValue));
        if ("".equals(intString)) {
            return defaultValue;
        }
        return Integer.parseInt(intString);
    }

    /**
     * Reads the int array from the properties.
     *
     * @param properties the properties.
     * @param keyword    the keyword.
     * @return the int array from the properties.
     */
    public static int[] readIntArray(Properties properties, String keyword) {
        String[] intStringArray = readTrimStringArray(properties, keyword);
        return Arrays.stream(intStringArray)
            .mapToInt(Integer::parseInt)
            .toArray();
    }

    /**
     * Reads the int array from the properties. If the keyword is not set, return int[0].
     *
     * @param properties the properties.
     * @param keyword    the keyword.
     * @return the int array from the properties.
     */
    public static int[] readIntArrayWithDefault(Properties properties, String keyword) {
        String[] intStringArray = readTrimStringArrayWithDefault(properties, keyword);
        return Arrays.stream(intStringArray)
            .mapToInt(Integer::parseInt)
            .toArray();
    }

    /**
     * Reads the log int array from the properties.
     *
     * @param properties the properties.
     * @param keyword    the keyword.
     * @return the log int array from the properties.
     */
    public static int[] readLogIntArray(Properties properties, String keyword) {
        String[] intLogStringArray = readTrimStringArray(properties, keyword);
        return Arrays.stream(intLogStringArray)
            .mapToInt(Integer::parseInt)
            .peek(logIntValue -> MathPreconditions.checkPositiveInRange("log(n)", logIntValue, Integer.SIZE))
            .toArray();
    }

    /**
     * Reads the double from the properties.
     *
     * @param properties the properties.
     * @param keyword    the keyword.
     * @return the double from the properties.
     */
    public static double readDouble(Properties properties, String keyword) {
        String doubleString = readString(properties, keyword);
        return Double.parseDouble(doubleString);
    }

    /**
     * Reads the double array from the properties.
     *
     * @param properties the properties.
     * @param keyword    the keyword.
     * @return the double array from the properties.
     */
    public static double[] readDoubleArray(Properties properties, String keyword) {
        String[] doubleStringArray = readTrimStringArray(properties, keyword);
        return Arrays.stream(doubleStringArray)
            .mapToDouble(Double::parseDouble)
            .toArray();
    }

    /**
     * Reads the double array from the properties. If the keyword is not set, return double[0].
     *
     * @param properties the properties.
     * @param keyword    the keyword.
     * @return the double array from the properties. If the keyword is not set, return double[0].
     */
    public static double[] readDoubleArrayWithDefault(Properties properties, String keyword) {
        String[] doubleStringArray = readTrimStringArrayWithDefault(properties, keyword);
        return Arrays.stream(doubleStringArray)
            .mapToDouble(Double::parseDouble)
            .toArray();
    }
}
