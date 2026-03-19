package edu.alibaba.mpc4j.work.db.sketch.HLL;

import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.db.sketch.HLL.v1.v1HLLConfig;
import edu.alibaba.mpc4j.work.db.sketch.HLL.v1.v1HLLParty;

/**
 * HLL factory.
 */
public class HLLFactory {
    public enum HLLPtoType {
        /**
         * v1 HLL only z2 version
         */
        V1,
    }

    public static HLLParty createHLLParty(Abb3Party abb3Party, HLLConfig config) {
        return switch (config.getPtoType()) {
            case V1 -> new v1HLLParty(abb3Party, (v1HLLConfig) config);
            default -> throw new IllegalArgumentException("Invalid config.getPtoType() in creating HLLParty");
        };
    }
}
