package edu.alibaba.mpc4j.work.scape.s3pc.db.main.semijoin.general;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.general.GeneralJoinFactory.GeneralJoinPtoType;
import edu.alibaba.mpc4j.work.scape.s3pc.db.main.join.general.GeneralJoinMain;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.general.GeneralSemiJoinConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.general.GeneralSemiJoinFactory.GeneralSemiJoinPtoType;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.general.hzf22.Hzf22GeneralSemiJoinConfig;

import java.util.Properties;

/**
 * general semi-join protocol configure utils
 *
 * @author Feng Han
 * @date 2025/2/28
 */
public class GeneralSemiJoinConfigUtils {
    /**
     * private constructor.
     */
    private GeneralSemiJoinConfigUtils() {
        // empty
    }

    /**
     * Creates config.
     *
     * @param properties properties.
     * @return config.
     */
    public static GeneralSemiJoinConfig createConfig(Properties properties) {
        GeneralSemiJoinPtoType joinPtoType = MainPtoConfigUtils.readEnum(GeneralSemiJoinPtoType.class, properties, GeneralJoinMain.PTO_NAME_KEY);
        return switch (joinPtoType) {
            case GENERAL_SEMI_JOIN_HZF22 -> generateHzf22GeneralSemiJoinConfig(properties);
            default ->
                throw new IllegalArgumentException("Invalid " + GeneralJoinPtoType.class.getSimpleName() + ": " + joinPtoType.name());
        };
    }

    private static Hzf22GeneralSemiJoinConfig generateHzf22GeneralSemiJoinConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, GeneralJoinMain.IS_MALICIOUS);
        return new Hzf22GeneralSemiJoinConfig.Builder(malicious).build();
    }
}
