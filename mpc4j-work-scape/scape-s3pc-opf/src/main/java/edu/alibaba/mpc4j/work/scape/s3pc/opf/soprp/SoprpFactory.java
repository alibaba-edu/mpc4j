package edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.lowmc.LowMcParamUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.lowmc.LowMcSoprpConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.lowmc.LowMcSoprpParty;

/**
 * 3p soprp party factory.
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public class SoprpFactory {
    /**
     * the protocol type
     */
    public enum SoprpType {
        /**
         * lowMc SoPRP
         */
        LOWMC_SOPRP,
    }

    /**
     * Creates a type conversion party.
     *
     * @param config    the config.
     * @param abb3Party abb3 party
     * @return a z2c party.
     */
    public static SoprpParty createParty(Abb3Party abb3Party, SoprpConfig config) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (config.getPrpType()) {
            case LOWMC_SOPRP:
                return new LowMcSoprpParty(abb3Party, (LowMcSoprpConfig) config);
            default:
                throw new IllegalArgumentException("Invalid config.getPrpType() in creating SoprpParty");
        }
    }

    public static SoprpConfig createDefaultConfig(SecurityModel securityModel, int bitLen) {
        return new LowMcSoprpConfig.Builder(securityModel.equals(SecurityModel.MALICIOUS),
            LowMcParamUtils.getParam(bitLen, 1 << 20, 40)).build();
    }
}
