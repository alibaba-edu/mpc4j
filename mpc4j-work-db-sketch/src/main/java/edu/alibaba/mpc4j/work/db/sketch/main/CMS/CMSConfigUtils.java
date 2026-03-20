package edu.alibaba.mpc4j.work.db.sketch.main.CMS;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.db.sketch.CMS.CMSConfig;
import edu.alibaba.mpc4j.work.db.sketch.CMS.CMSFactory.CMSPtoType;
import edu.alibaba.mpc4j.work.db.sketch.CMS.z2.CMSz2Config;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory.PgSortType;

import java.util.Properties;
/**
 * Configuration utilities for CMS protocol.
 * <p>
 * This class provides factory methods for creating CMS protocol configurations
 * from property files. It supports different CMS implementations and protocol variants.
 * </p>
 */
public class CMSConfigUtils {
    /**
     * Configuration key for specifying the sort protocol type
     */
    private static final String SORT_TYPE = "sort_pto_type";
    /**
     * Configuration key for specifying whether to run in malicious security model
     */
    private static final String MALICIOUS = "malicious";

    /**
     * Private constructor to prevent instantiation.
     */
    private CMSConfigUtils() {
        // empty
    }

    /**
     * Creates a CMS configuration from the given properties.
     * <p>
     * Reads the protocol type from properties and dispatches to the appropriate
     * configuration builder based on the type.
     * </p>
     *
     * @param properties configuration properties containing protocol parameters
     * @return CMS configuration object
     */
    public static CMSConfig createConfig(Properties properties) {
        CMSPtoType cmsPtoType = MainPtoConfigUtils.readEnum(CMSPtoType.class, properties, CMSZ2Main.PTO_NAME_KEY);
        return switch (cmsPtoType) {
            case CMS_Z2 -> generateV2CmsConfig(properties);
            default ->
                    throw new IllegalArgumentException("Invalid " + GroupSumFactory.GroupSumPtoType.class.getSimpleName() + ": " + cmsPtoType.name());
        };
    }

    /**
     * Generates a CMS Z2 configuration from properties.
     * <p>
     * Creates configuration for the Z2-based CMS implementation including
     * security model (malicious or semi-honest) and sorting protocol settings.
     * </p>
     *
     * @param properties configuration properties
     * @return CMS Z2 configuration
     */
    private static CMSz2Config generateV2CmsConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, MALICIOUS);
        PgSortType sortType = MainPtoConfigUtils.readEnum(PgSortType.class, properties, SORT_TYPE);
        PgSortConfig sortConfig = PgSortFactory.createSortConfig(sortType, malicious);
        return new CMSz2Config.Builder(malicious).setPgSortConfig(sortConfig).build();
    }

}
