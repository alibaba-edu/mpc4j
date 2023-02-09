package edu.alibaba.mpc4j.common.data;

import com.google.common.base.Preconditions;
import org.apache.commons.csv.CSVFormat;

/**
 * Dataset manager.
 *
 * @author Weiran Liu
 * @date 2021/10/03
 */
public class DatasetManager {
    /**
     * path prefix
     */
    public static String pathPrefix = "../data/";
    /**
     * the default CSV formart
     */
    public static final CSVFormat DEFAULT_CSV_FORMAT = CSVFormat.Builder.create()
        .setHeader()
        .setIgnoreHeaderCase(true)
        .build();

    private DatasetManager() {
        // empty
    }

    /**
     * Set the path prefix.
     *
     * @param pathPrefix the path prefix.
     */
    public static void setPathPrefix(String pathPrefix) {
        Preconditions.checkArgument(pathPrefix.endsWith("/"), "Path Prefix must end with '/': %s", pathPrefix);
        DatasetManager.pathPrefix = pathPrefix;
    }
}
