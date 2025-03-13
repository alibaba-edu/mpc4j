package edu.alibaba.mpc4j.work.scape.s3pc.db.main.join.general;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.general.GeneralJoinConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.general.GeneralJoinFactory.GeneralJoinPtoType;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.general.hzf22.Hzf22GeneralJoinConfig;

import java.util.Properties;

/**
 * general join protocol configure utils
 *
 * @author Feng Han
 * @date 2025/2/28
 */
public class GeneralJoinConfigUtils {
    /**
     * private constructor.
     */
    private GeneralJoinConfigUtils() {
        // empty
    }

    /**
     * Creates config.
     *
     * @param properties properties.
     * @return config.
     */
    public static GeneralJoinConfig createConfig(Properties properties) {
        GeneralJoinPtoType joinPtoType = MainPtoConfigUtils.readEnum(GeneralJoinPtoType.class, properties, GeneralJoinMain.PTO_NAME_KEY);
        return switch (joinPtoType) {
            case GENERAL_JOIN_HZF22 -> generateHzf22GeneralJoinConfig(properties);
            default ->
                throw new IllegalArgumentException("Invalid " + GeneralJoinPtoType.class.getSimpleName() + ": " + joinPtoType.name());
        };
    }

    private static Hzf22GeneralJoinConfig generateHzf22GeneralJoinConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, GeneralJoinMain.IS_MALICIOUS);
        return new Hzf22GeneralJoinConfig.Builder(malicious).build();
    }
}
