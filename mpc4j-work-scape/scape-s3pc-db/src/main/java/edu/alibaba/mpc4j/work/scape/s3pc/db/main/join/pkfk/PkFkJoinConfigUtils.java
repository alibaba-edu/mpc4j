package edu.alibaba.mpc4j.work.scape.s3pc.db.main.join.pkfk;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkfk.PkFkJoinConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkfk.PkFkJoinFactory.PkFkJoinPtoType;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkfk.hzf22.Hzf22PkFkJoinConfig;

import java.util.Properties;

/**
 * Pk-Fk join protocol configure utils
 *
 * @author Feng Han
 * @date 2025/2/28
 */
public class PkFkJoinConfigUtils {
    /**
     * private constructor.
     */
    private PkFkJoinConfigUtils() {
        // empty
    }

    /**
     * Creates config.
     *
     * @param properties properties.
     * @return config.
     */
    public static PkFkJoinConfig createConfig(Properties properties) {
        PkFkJoinPtoType pkFkJoinPtoType = MainPtoConfigUtils.readEnum(PkFkJoinPtoType.class, properties, PkFkJoinMain.PTO_NAME_KEY);
        return switch (pkFkJoinPtoType) {
            case PK_FK_JOIN_HZF22 -> generateHzf22PkFkJoinConfig(properties);
            default ->
                throw new IllegalArgumentException("Invalid " + PkFkJoinPtoType.class.getSimpleName() + ": " + pkFkJoinPtoType.name());
        };
    }

    private static Hzf22PkFkJoinConfig generateHzf22PkFkJoinConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, PkFkJoinMain.IS_MALICIOUS);
        return new Hzf22PkFkJoinConfig.Builder(malicious).build();
    }
}
