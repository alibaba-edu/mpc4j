package edu.alibaba.mpc4j.common.data;

import org.apache.commons.csv.CSVFormat;

/**
 * 数据集管理器。
 *
 * @author Weiran Liu
 * @date 2021/10/03
 */
public class DatasetManager {
    /**
     * 路径前缀
     */
    public static String pathPrefix = "../data/";
    /**
     * 默认CSV格式
     */
    public static final CSVFormat DEFAULT_CSV_FORMAT = CSVFormat.Builder.create()
        .setHeader()
        .setIgnoreHeaderCase(true)
        .build();

    private DatasetManager() {
        // empty
    }

    /**
     * 设置路径前缀。
     *
     * @param pathPrefix 路径前缀。
     */
    public static void setPathPrefix(String pathPrefix) {
        assert pathPrefix.endsWith("/") : "Path Prefix must end with '/': " + pathPrefix;
        DatasetManager.pathPrefix = pathPrefix;
    }
}
