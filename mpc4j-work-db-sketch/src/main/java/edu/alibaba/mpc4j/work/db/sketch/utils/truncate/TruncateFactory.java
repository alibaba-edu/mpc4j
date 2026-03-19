package edu.alibaba.mpc4j.work.db.sketch.utils.truncate;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.db.sketch.utils.truncate.ext.ExtTruncateConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.truncate.ext.ExtTruncateParty;

public class TruncateFactory {
    public enum TruncatePtoType {
        /**
         * EXT, the method uses oblivious permutation
         */
        EXT
    }

    /**
     * Creates a general join party.
     *
     * @param config    the config.
     * @param abb3Party abb3 party
     * @return a general join party.
     */
    public static TruncateParty createParty(Abb3Party abb3Party, TruncateConfig config) {
        return switch (config.getPtoType()) {
            case EXT -> new ExtTruncateParty(abb3Party, (ExtTruncateConfig) config);
            default -> throw new IllegalArgumentException("Invalid config.getPtoType() in creating TruncateParty");
        };
    }

    public static TruncateConfig createDefaultConfig(SecurityModel securityModel) {
        return new ExtTruncateConfig.Builder(securityModel.equals(SecurityModel.MALICIOUS)).build();
    }
}
