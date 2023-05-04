package edu.alibaba.mpc4j.s2pc.pir.index.fastpir;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirFactory;

/**
 * FastPIR config.
 *
 * @author Liqiang Peng
 * @date 2023/1/18
 */
public class Ayaa21IndexPirConfig implements IndexPirConfig {

    public Ayaa21IndexPirConfig() {
        // empty
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }

    @Override
    public void setEnvType(EnvType envType) {
        if (envType.equals(EnvType.STANDARD_JDK) || envType.equals(EnvType.INLAND_JDK)) {
            throw new IllegalArgumentException("Protocol using " + CommonConstants.MPC4J_NATIVE_FHE_NAME
                + " must not be " + EnvType.STANDARD_JDK.name() + " or " + EnvType.INLAND_JDK.name()
                + ": " + envType.name());
        }
    }

    @Override
    public EnvType getEnvType() {
        return EnvType.STANDARD;
    }

    @Override
    public IndexPirFactory.IndexPirType getProType() {
        return IndexPirFactory.IndexPirType.FAST_PIR;
    }
}
