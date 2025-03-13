package edu.alibaba.mpc4j.work.scape.s3pc.db.main.join.pkpk;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.PkPkJoinConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.PkPkJoinFactory.PkPkJoinPtoType;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.hzf22.Hzf22PkPkJoinConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.mrr20.Mrr20PkPkJoinConfig;

import java.util.Properties;

/**
 * pkpk join protocol configure utils
 *
 * @author Feng Han
 * @date 2025/2/28
 */
public class PkPkJoinConfigUtils {
    /**
     * comparator type key.
     */
    private static final String COMPARATOR_TYPE = "comparator_type";

    /**
     * private constructor.
     */
    private PkPkJoinConfigUtils() {
        // empty
    }

    /**
     * Creates config.
     *
     * @param properties properties.
     * @return config.
     */
    public static PkPkJoinConfig createConfig(Properties properties) {
        PkPkJoinPtoType pkPkJoinPtoType = MainPtoConfigUtils.readEnum(PkPkJoinPtoType.class, properties, PkPkJoinMain.PTO_NAME_KEY);
        return switch (pkPkJoinPtoType) {
            case PK_PK_JOIN_HZF22 -> generateHzf22PkPkJoinConfig(properties);
            case PK_PK_JOIN_MRR20 -> generateMrr20PkPkJoinConfig(properties);
            default ->
                throw new IllegalArgumentException("Invalid " + PkPkJoinPtoType.class.getSimpleName() + ": " + pkPkJoinPtoType.name());
        };
    }

    private static Hzf22PkPkJoinConfig generateHzf22PkPkJoinConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, PkPkJoinMain.IS_MALICIOUS);
        ComparatorType comparatorType = MainPtoConfigUtils.readEnum(ComparatorType.class, properties, COMPARATOR_TYPE);
        return new Hzf22PkPkJoinConfig.Builder(malicious).setComparatorType(comparatorType).build();
    }

    private static Mrr20PkPkJoinConfig generateMrr20PkPkJoinConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, PkPkJoinMain.IS_MALICIOUS);
        return new Mrr20PkPkJoinConfig.Builder(malicious).build();
    }
}
