package edu.alibaba.mpc4j.work.db.sketch.main.GK;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.db.sketch.GK.GKConfig;
import edu.alibaba.mpc4j.work.db.sketch.GK.GKFactory;
import edu.alibaba.mpc4j.work.db.sketch.GK.v1.v1GKConfig;

import java.util.Properties;

public class GKConfigUtils {
    /**
     * PgSort pto name
     */
    private static final String SORT_TYPE = "sort_pto_type";

    /**
     * private constructor.
     */
    private GKConfigUtils() {
        // empty
    }

    /**
     * Creates config.
     *
     * @param properties properties.
     * @return config.
     */
    public static GKConfig createConfig(Properties properties) {
        GKFactory.GKPtoType gkPtoType = MainPtoConfigUtils.readEnum(GKFactory.GKPtoType.class, properties, GKMain.PTO_NAME_KEY);
        return switch (gkPtoType) {
            // todo change
            case V1 -> generateV1GKConfig(properties);
            default ->
                throw new IllegalArgumentException("Invalid " + GroupSumFactory.GroupSumPtoType.class.getSimpleName() + ": " + gkPtoType.name());
        };
    }

    private static v1GKConfig generateV1GKConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, GKMain.IS_MALICIOUS);
        PgSortFactory.PgSortType sortType = MainPtoConfigUtils.readEnum(PgSortFactory.PgSortType.class, properties, SORT_TYPE);
        PgSortConfig sortConfig = PgSortFactory.createSortConfig(sortType, malicious);
        return new v1GKConfig.Builder(malicious).setPgSortConfig(sortConfig).build();
    }
}
