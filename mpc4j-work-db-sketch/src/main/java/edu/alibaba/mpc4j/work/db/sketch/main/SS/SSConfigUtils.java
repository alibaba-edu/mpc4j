package edu.alibaba.mpc4j.work.db.sketch.main.SS;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.db.sketch.SS.SSConfig;
import edu.alibaba.mpc4j.work.db.sketch.SS.SSFactory;
import edu.alibaba.mpc4j.work.db.sketch.SS.v1.v1SSConfig;

import java.util.Properties;

public class SSConfigUtils {
    /**
     * PgSort pto name
     */
    private static final String SORT_TYPE = "sort_pto_type";

    /**
     * private constructor.
     */
    private SSConfigUtils() {
        // empty
    }

    /**
     * Creates config.
     *
     * @param properties properties.
     * @return config.
     */
    public static SSConfig createConfig(Properties properties) {
        SSFactory.MGPtoType mgPtoType = MainPtoConfigUtils.readEnum(SSFactory.MGPtoType.class, properties, SSMain.PTO_NAME_KEY);
        return switch (mgPtoType) {
            // todo change
            case V1 -> generateV1MgConfig(properties);
            default ->
                    throw new IllegalArgumentException("Invalid " + GroupSumFactory.GroupSumPtoType.class.getSimpleName() + ": " + mgPtoType.name());
        };
    }

    private static v1SSConfig generateV1MgConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, SSMain.IS_MALICIOUS);
        PgSortFactory.PgSortType sortType = MainPtoConfigUtils.readEnum(PgSortFactory.PgSortType.class, properties, SORT_TYPE);
        PgSortConfig sortConfig = PgSortFactory.createSortConfig(sortType, malicious);
        return new v1SSConfig.Builder(malicious).setPgSortConfig(sortConfig).build();
    }
}
