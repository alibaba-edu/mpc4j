package edu.alibaba.mpc4j.work.db.sketch.main.HLL;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.db.sketch.HLL.HLLConfig;
import edu.alibaba.mpc4j.work.db.sketch.HLL.HLLFactory.HLLPtoType;
import edu.alibaba.mpc4j.work.db.sketch.HLL.z2.HLLz2Config;

import java.util.Properties;

/**
 * Configuration utilities for HLL protocol.
 * <p>
 * Provides factory methods for creating HLL protocol configurations
 * from property files, supporting different implementation variants.
 * </p>
 */
public class HLLConfigUtils {
    /**
     * Configuration key for specifying the sort protocol type
     */
    private static final String SORT_TYPE = "sort_pto_type";
    
    /**
     * Private constructor to prevent instantiation.
     */
    private HLLConfigUtils() {

    }

    /**
     * Creates an HLL configuration from the given properties.
     * <p>
     * Reads the protocol type and dispatches to the appropriate
     * configuration builder.
     * </p>
     *
     * @param properties configuration properties
     * @return HLL configuration object
     */
    public static HLLConfig createConfig(Properties properties) {
        HLLPtoType hllPtoType = MainPtoConfigUtils.readEnum(HLLPtoType.class,properties,HLLMain.PTO_NAME_KEY);
        return switch(hllPtoType){
            case Z2 -> generateHLLConfig(properties);
            default ->
                    throw new IllegalArgumentException();
        };
    }
    
    /**
     * Generates an HLL Z2 configuration from properties.
     * <p>
     * Creates configuration for the Z2-based HLL implementation including
     * security model and sorting protocol settings.
     * </p>
     *
     * @param properties configuration properties
     * @return HLL Z2 configuration
     */
    private static HLLConfig generateHLLConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, HLLMain.IS_MALICIOUS);
        PgSortFactory.PgSortType sortType = MainPtoConfigUtils.readEnum(PgSortFactory.PgSortType.class, properties, SORT_TYPE);
        PgSortConfig sortConfig = PgSortFactory.createSortConfig(sortType, malicious);
        return new HLLz2Config.Builder(malicious).setPgSortConfig(sortConfig).build();
    }
}
