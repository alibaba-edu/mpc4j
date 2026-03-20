package edu.alibaba.mpc4j.work.db.sketch.main.GK;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.db.sketch.GK.GKConfig;
import edu.alibaba.mpc4j.work.db.sketch.GK.GKFactory;
import edu.alibaba.mpc4j.work.db.sketch.GK.z2.GKz2Config;

import java.util.Properties;

/**
 * Configuration utilities for GK protocol.
 * <p>
 * Provides factory methods for creating GK protocol configurations
 * from property files, supporting different implementation variants.
 * </p>
 */
public class GKConfigUtils {
    /**
     * Configuration key for specifying the sort protocol type
     */
    private static final String SORT_TYPE = "sort_pto_type";

    /**
     * Private constructor to prevent instantiation.
     */
    private GKConfigUtils() {
        // empty
    }

    /**
     * Creates a GK configuration from the given properties.
     * <p>
     * Reads the protocol type and dispatches to the appropriate
     * configuration builder.
     * </p>
     *
     * @param properties configuration properties
     * @return GK configuration object
     */
    public static GKConfig createConfig(Properties properties) {
        GKFactory.GKPtoType gkPtoType = MainPtoConfigUtils.readEnum(GKFactory.GKPtoType.class, properties, GKMain.PTO_NAME_KEY);
        return switch (gkPtoType) {
            case Z2 -> generateV1GKConfig(properties);
            default ->
                throw new IllegalArgumentException("Invalid " + GroupSumFactory.GroupSumPtoType.class.getSimpleName() + ": " + gkPtoType.name());
        };
    }

    /**
     * Generates a GK Z2 configuration from properties.
     * <p>
     * Creates configuration for the Z2-based GK implementation including
     * security model and sorting protocol settings.
     * </p>
     *
     * @param properties configuration properties
     * @return GK Z2 configuration
     */
    private static GKz2Config generateV1GKConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, GKMain.IS_MALICIOUS);
        PgSortFactory.PgSortType sortType = MainPtoConfigUtils.readEnum(PgSortFactory.PgSortType.class, properties, SORT_TYPE);
        PgSortConfig sortConfig = PgSortFactory.createSortConfig(sortType, malicious);
        return new GKz2Config.Builder(malicious).setPgSortConfig(sortConfig).build();
    }
}
