package edu.alibaba.mpc4j.work.scape.s3pc.opf.main.permutation;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory.PermuteType;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.ahi22.Ahi22PermuteConfig;

import java.util.Properties;

/**
 * configure utils for oblivious permutation
 *
 * @author Feng Han
 * @date 2025/2/28
 */
public class PermutationConfigUtils {
    /**
     * private constructor.
     */
    private PermutationConfigUtils() {
        // empty
    }

    /**
     * Creates config.
     *
     * @param properties properties.
     * @return config.
     */
    public static PermuteConfig createConfig(Properties properties) {
        PermuteType permuteType = MainPtoConfigUtils.readEnum(PermuteType.class, properties, PermutationMain.PTO_NAME_KEY);
        switch (permuteType) {
            case PERMUTE_AHI22:
                return generateAhi22PermuteConfig(properties);
            default:
                throw new IllegalArgumentException("Invalid " + PermuteType.class.getSimpleName() + ": " + permuteType.name());
        }
    }

    private static Ahi22PermuteConfig generateAhi22PermuteConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, PermutationMain.IS_MALICIOUS);
        return new Ahi22PermuteConfig.Builder(malicious).build();
    }
}
