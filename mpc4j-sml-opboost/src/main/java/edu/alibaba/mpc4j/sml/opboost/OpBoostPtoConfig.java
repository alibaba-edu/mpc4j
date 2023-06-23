package edu.alibaba.mpc4j.sml.opboost;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * OpBoost protocol config.
 *
 * @author Weiran Liu
 * @date 2023/2/12
 */
class OpBoostPtoConfig implements MultiPartyPtoConfig {

    OpBoostPtoConfig() {
        // empty
    }

    @Override
    public void setEnvType(EnvType envType) {
        // do not need to set the environment
    }

    @Override
    public EnvType getEnvType() {
        return EnvType.STANDARD_JDK;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }
}
