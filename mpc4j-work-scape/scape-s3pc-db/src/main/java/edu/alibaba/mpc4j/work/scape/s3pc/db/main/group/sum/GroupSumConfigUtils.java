package edu.alibaba.mpc4j.work.scape.s3pc.db.main.group.sum;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumFactory.GroupSumPtoType;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.hzf22.Hzf22GroupSumConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.hzf22ext.Hzf22ExtGroupSumConfig;

import java.util.Properties;

/**
 * group sum configure utils
 *
 * @author Feng Han
 * @date 2025/2/28
 */
public class GroupSumConfigUtils {
    /**
     * comparator type key.
     */
    private static final String COMPARATOR_TYPE = "comparator_type";

    /**
     * private constructor.
     */
    private GroupSumConfigUtils() {
        // empty
    }

    /**
     * Creates config.
     *
     * @param properties properties.
     * @return config.
     */
    public static GroupSumConfig createConfig(Properties properties) {
        GroupSumPtoType groupSumPtoType = MainPtoConfigUtils.readEnum(GroupSumPtoType.class, properties, GroupSumMain.PTO_NAME_KEY);
        return switch (groupSumPtoType) {
            case HZF22 -> generateHzf22GroupSumConfig(properties);
            case HZF22EXT -> generateHzf22ExtGroupSumConfig(properties);
            default ->
                throw new IllegalArgumentException("Invalid " + GroupSumPtoType.class.getSimpleName() + ": " + groupSumPtoType.name());
        };
    }

    private static Hzf22GroupSumConfig generateHzf22GroupSumConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, GroupSumMain.IS_MALICIOUS);
        ComparatorType comparatorType = MainPtoConfigUtils.readEnum(ComparatorType.class, properties, COMPARATOR_TYPE);
        return new Hzf22GroupSumConfig.Builder(malicious).setComparatorType(comparatorType).build();
    }

    private static Hzf22ExtGroupSumConfig generateHzf22ExtGroupSumConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, GroupSumMain.IS_MALICIOUS);
        ComparatorType comparatorType = MainPtoConfigUtils.readEnum(ComparatorType.class, properties, COMPARATOR_TYPE);
        return new Hzf22ExtGroupSumConfig.Builder(malicious).setComparatorType(comparatorType).build();
    }
}
