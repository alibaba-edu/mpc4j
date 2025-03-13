package edu.alibaba.mpc4j.work.scape.s3pc.db.main.group.extreme;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.GroupExtremeConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.GroupExtremeFactory.GroupExtremePtoType;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.hzf22.Hzf22GroupExtremeConfig;

import java.util.Properties;

/**
 * group extreme configure utils
 *
 * @author Feng Han
 * @date 2025/2/28
 */
public class GroupExtremeConfigUtils {
    /**
     * comparator type key.
     */
    private static final String COMPARATOR_TYPE = "comparator_type";

    /**
     * private constructor.
     */
    private GroupExtremeConfigUtils() {
        // empty
    }

    /**
     * Creates config.
     *
     * @param properties properties.
     * @return config.
     */
    public static GroupExtremeConfig createConfig(Properties properties) {
        GroupExtremePtoType groupExtremePtoType = MainPtoConfigUtils.readEnum(GroupExtremePtoType.class, properties, GroupExtremeMain.PTO_NAME_KEY);
        switch (groupExtremePtoType) {
            case HZF22:
                return generateHzf22GroupExtremeConfig(properties);
            default:
                throw new IllegalArgumentException("Invalid " + GroupExtremePtoType.class.getSimpleName() + ": " + groupExtremePtoType.name());
        }
    }

    private static Hzf22GroupExtremeConfig generateHzf22GroupExtremeConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, GroupExtremeMain.IS_MALICIOUS);
        ComparatorType comparatorType = MainPtoConfigUtils.readEnum(ComparatorType.class, properties, COMPARATOR_TYPE);
        return new Hzf22GroupExtremeConfig.Builder(malicious).setComparatorType(comparatorType).build();
    }
}
