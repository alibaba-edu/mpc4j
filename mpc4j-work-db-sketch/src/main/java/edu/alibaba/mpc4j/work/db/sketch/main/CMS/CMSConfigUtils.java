package edu.alibaba.mpc4j.work.db.sketch.main.CMS;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory.PgSortType;
import edu.alibaba.mpc4j.work.db.sketch.CMS.CMSConfig;
import edu.alibaba.mpc4j.work.db.sketch.CMS.CMSFactory.CMSPtoType;
import edu.alibaba.mpc4j.work.db.sketch.CMS.v2.v2CMSConfig;

import java.util.Properties;

/**
 * config utilities for v1 CMS protocol
 */
public class CMSConfigUtils {
    /**
     * PgSort pto name
     */
    private static final String SORT_TYPE = "sort_pto_type";
    /**
     * is malicious or not
     */
    private static final String MALICIOUS = "malicious";

    /**
     * private constructor.
     */
    private CMSConfigUtils() {
        // empty
    }

    /**
     * Creates config.
     *
     * @param properties properties.
     * @return config.
     */
    public static CMSConfig createConfig(Properties properties) {
        CMSPtoType cmsPtoType = MainPtoConfigUtils.readEnum(CMSPtoType.class, properties, CMSZ2Main.PTO_NAME_KEY);
        return switch (cmsPtoType) {
            case CMS_V2 -> generateV2CmsConfig(properties);
            default ->
                    throw new IllegalArgumentException("Invalid " + GroupSumFactory.GroupSumPtoType.class.getSimpleName() + ": " + cmsPtoType.name());
        };
    }

    private static v2CMSConfig generateV2CmsConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, MALICIOUS);
        PgSortType sortType = MainPtoConfigUtils.readEnum(PgSortType.class, properties, SORT_TYPE);
        PgSortConfig sortConfig = PgSortFactory.createSortConfig(sortType, malicious);
        return new v2CMSConfig.Builder(malicious).setPgSortConfig(sortConfig).build();
    }


}
