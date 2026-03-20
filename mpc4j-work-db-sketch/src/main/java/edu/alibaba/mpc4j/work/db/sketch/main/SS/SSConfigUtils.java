package edu.alibaba.mpc4j.work.db.sketch.main.SS;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.db.sketch.SS.SSFactory.SSPtoType;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.db.sketch.SS.SSConfig;
import edu.alibaba.mpc4j.work.db.sketch.SS.z2.SSz2Config;

import java.util.Properties;

/**
 * Configuration utilities for SS protocol.
 * <p>
 * Provides factory methods for creating SS protocol configurations
 * from property files, supporting different implementation variants.
 * </p>
 */
public class SSConfigUtils {
    /**
     * Configuration key for specifying the sort protocol type
     */
    private static final String SORT_TYPE = "sort_pto_type";

    /**
     * Private constructor to prevent instantiation.
     */
    private SSConfigUtils() {
        // empty
    }

    /**
     * Creates an SS configuration from the given properties.
     * <p>
     * Reads the protocol type and dispatches to the appropriate
     * configuration builder.
     * </p>
     *
     * @param properties configuration properties
     * @return SS configuration object
     */
    public static SSConfig createConfig(Properties properties) {
        SSPtoType SSPtoType = MainPtoConfigUtils.readEnum(SSPtoType.class, properties, SSMain.PTO_NAME_KEY);
        return switch (SSPtoType) {
            case Z2 -> generateV1MgConfig(properties);
            default ->
                    throw new IllegalArgumentException("Invalid " + GroupSumFactory.GroupSumPtoType.class.getSimpleName() + ": " + SSPtoType.name());
        };
    }

    /**
     * Generates an SS Z2 configuration from properties.
     * <p>
     * Creates configuration for the Z2-based SS implementation including
     * security model and sorting protocol settings.
     * </p>
     *
     * @param properties configuration properties
     * @return SS Z2 configuration
     */
    private static SSz2Config generateV1MgConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, SSMain.IS_MALICIOUS);
        PgSortFactory.PgSortType sortType = MainPtoConfigUtils.readEnum(PgSortFactory.PgSortType.class, properties, SORT_TYPE);
        PgSortConfig sortConfig = PgSortFactory.createSortConfig(sortType, malicious);
        return new SSz2Config.Builder(malicious).setPgSortConfig(sortConfig).build();
    }
}
