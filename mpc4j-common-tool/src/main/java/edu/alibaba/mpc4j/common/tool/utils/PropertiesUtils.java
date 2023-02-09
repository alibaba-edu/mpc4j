package edu.alibaba.mpc4j.common.tool.utils;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * 参数设置工具类。
 *
 * @author Weiran Liu
 * @date 2022/8/28
 */
public class PropertiesUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesUtils.class);

    private PropertiesUtils() {
        // empty
    }

    /**
     * 从配置文件中读取参数。
     *
     * @param file 配置文件。
     * @return 参数。
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
     * 判断是否设置了指定关键字的属性。
     *
     * @param properties 配置项。
     * @param keyword 关键字。
     * @return 是否设置了此关键字的属性。
     */
    public static boolean containsKeyword(Properties properties, String keyword) {
        String readString = properties.getProperty(keyword);
        return !Objects.isNull(readString);
    }

    /**
     * 读取字符串。
     *
     * @param properties 配置项。
     * @param keyword    关键字。
     * @return 字符串。
     */
    public static String readString(Properties properties, String keyword) {
        String readString = Preconditions.checkNotNull(
            properties.getProperty(keyword), "Please set " + keyword
        );
        LOGGER.info("{} is set, value: {}", keyword, readString);
        return readString;
    }

    /**
     * 读取字符串。
     *
     * @param properties   配置项。
     * @param keyword      关键字。
     * @param defaultValue 默认值。
     * @return 字符串。
     */
    public static String readString(Properties properties, String keyword, String defaultValue) {
        String readString = properties.getProperty(keyword);
        if (readString == null) {
            LOGGER.info("{} is not set, choose default value: {}", keyword, defaultValue);
            return defaultValue;
        } else {
            LOGGER.info("{} is set, value: {}", keyword, readString);
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
     * 读取布尔值。
     *
     * @param properties 配置项。
     * @param keyword    关键字。
     * @return 布尔值。
     */
    public static boolean readBoolean(Properties properties, String keyword) {
        String booleanString = readString(properties, keyword);
        return Boolean.parseBoolean(booleanString);
    }

    /**
     * 读取布尔值。
     *
     * @param properties   配置项。
     * @param keyword      关键字。
     * @param defaultValue 默认值。
     * @return 布尔值。
     */
    public static boolean readBoolean(Properties properties, String keyword, boolean defaultValue) {
        String booleanString = readString(properties, keyword);
        if (booleanString == null) {
            LOGGER.info("{} is not set, choose default value: {}", keyword, defaultValue);
            return defaultValue;
        } else {
            boolean booleanValue = Boolean.parseBoolean(booleanString);
            LOGGER.info("{} is set, value: {}", keyword, booleanValue);
            return booleanValue;
        }
    }

    /**
     * 读取整数。
     *
     * @param properties 配置项。
     * @param keyword    关键字。
     * @return 整数。
     */
    public static int readInt(Properties properties, String keyword) {
        String intString = readString(properties, keyword);
        return Integer.parseInt(intString);
    }

    /**
     * 读取整数数组。
     *
     * @param properties 配置项。
     * @param keyword    关键字。
     * @return 整数数组。
     */
    public static int[] readIntArray(Properties properties, String keyword) {
        String intArrayString = readString(properties, keyword);
        return Arrays.stream(intArrayString.split(","))
            .mapToInt(Integer::parseInt)
            .toArray();
    }

    /**
     * 读取对数整数数组。
     *
     * @param properties 配置项。
     * @param keyword    关键字。
     * @return 整数数组。
     */
    public static int[] readLogIntArray(Properties properties, String keyword) {
        String intArrayString = readString(properties, keyword);
        return Arrays.stream(intArrayString.split(","))
            .mapToInt(Integer::parseInt)
            .peek(logIntValue -> Preconditions.checkArgument(
                logIntValue > 0 && logIntValue < Integer.SIZE,
                "Log int value must be in range (%s, %s)", 0, Integer.SIZE))
            .toArray();
    }

    /**
     * 读取浮点数。
     *
     * @param properties 配置项。
     * @param keyword    关键字。
     * @return 浮点数。
     */
    public static double readDouble(Properties properties, String keyword) {
        String doubleString = readString(properties, keyword);
        return Double.parseDouble(doubleString);
    }

    /**
     * 读取浮点数数组。
     *
     * @param properties 配置项。
     * @param keyword    关键字。
     * @return 浮点数数组。
     */
    public static double[] readDoubleArray(Properties properties, String keyword) {
        String doubleString = readString(properties, keyword);
        return Arrays.stream(doubleString.split(","))
            .mapToDouble(Double::parseDouble)
            .toArray();
    }
}
