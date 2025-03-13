package edu.alibaba.mpc4j.work.scape.s3pc.db.main.semijoin.pkpk;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.pkpk.PkPkSemiJoinConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.pkpk.PkPkSemiJoinFactory.PkPkSemiJoinPtoType;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.pkpk.hzf22.Hzf22PkPkSemiJoinConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.pkpk.mrr20.Mrr20PkPkSemiJoinConfig;

import java.util.Properties;

/**
 * pkpk semi join protocol configure utils
 *
 * @author Feng Han
 * @date 2025/3/3
 */
public class PkPkSemiJoinConfigUtils {
    /**
     * comparator type key.
     */
    private static final String COMPARATOR_TYPE = "comparator_type";

    /**
     * private constructor.
     */
    private PkPkSemiJoinConfigUtils() {
        // empty
    }

    /**
     * Creates config.
     *
     * @param properties properties.
     * @return config.
     */
    public static PkPkSemiJoinConfig createConfig(Properties properties) {
        PkPkSemiJoinPtoType ptoType = MainPtoConfigUtils.readEnum(PkPkSemiJoinPtoType.class, properties, PkPkSemiJoinMain.PTO_NAME_KEY);
        return switch (ptoType) {
            case PK_PK_SEMI_JOIN_HZF22 -> generateHzf22PkPkSemiJoinConfig(properties);
            case PK_PK_SEMI_JOIN_MRR20 -> generateMrr20PkPkSemiJoinConfig(properties);
            default ->
                throw new IllegalArgumentException("Invalid " + PkPkSemiJoinPtoType.class.getSimpleName() + ": " + ptoType.name());
        };
    }

    private static Hzf22PkPkSemiJoinConfig generateHzf22PkPkSemiJoinConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, PkPkSemiJoinMain.IS_MALICIOUS);
        ComparatorType comparatorType = MainPtoConfigUtils.readEnum(ComparatorType.class, properties, COMPARATOR_TYPE);
        return new Hzf22PkPkSemiJoinConfig.Builder(malicious).build();
    }

    private static Mrr20PkPkSemiJoinConfig generateMrr20PkPkSemiJoinConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, PkPkSemiJoinMain.IS_MALICIOUS);
        return new Mrr20PkPkSemiJoinConfig.Builder(malicious).build();
    }
}
