package edu.alibaba.mpc4j.work.scape.s3pc.opf.main.agg;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.AggConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.AggFactory.AggPtoType;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.hzf22.Hzf22AggConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.main.permutation.PermutationMain;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory.PermuteType;

import java.util.Properties;

/**
 * agg configure utils
 *
 * @author Feng Han
 * @date 2025/2/28
 */
public class AggConfigUtils {
    /**
     * comparator type key.
     */
    private static final String COMPARATOR_TYPE = "comparator_type";

    /**
     * private constructor.
     */
    private AggConfigUtils() {
        // empty
    }

    /**
     * Creates config.
     *
     * @param properties properties.
     * @return config.
     */
    public static AggConfig createConfig(Properties properties) {
        AggPtoType aggPtoType = MainPtoConfigUtils.readEnum(AggPtoType.class, properties, AggMain.PTO_NAME_KEY);
        switch (aggPtoType) {
            case HZF22:
                return generateHzf22AggConfig(properties);
            default:
                throw new IllegalArgumentException("Invalid " + PermuteType.class.getSimpleName() + ": " + aggPtoType.name());
        }
    }

    private static Hzf22AggConfig generateHzf22AggConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, PermutationMain.IS_MALICIOUS);
        ComparatorType comparatorType = MainPtoConfigUtils.readEnum(ComparatorType.class, properties, COMPARATOR_TYPE);
        return new Hzf22AggConfig.Builder(malicious).setComparatorType(comparatorType).build();
    }
}
