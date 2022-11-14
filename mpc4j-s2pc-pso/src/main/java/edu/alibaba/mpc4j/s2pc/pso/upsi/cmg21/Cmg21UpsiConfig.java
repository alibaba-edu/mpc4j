package edu.alibaba.mpc4j.s2pc.pso.upsi.cmg21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pso.oprf.MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.upsi.UpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.upsi.UpsiFactory.UpsiType;

/**
 * CMG21协议配置项。
 *
 * @author Liqiang Peng
 * @date 2022/6/13
 */
public class Cmg21UpsiConfig implements UpsiConfig {
    /**
     * MP-OPRF协议
     */
    private final MpOprfConfig mpOprfConfig;

    public Cmg21UpsiConfig(Builder builder) {
        mpOprfConfig = builder.mpOprfConfig;
        EnvType envType = mpOprfConfig.getEnvType();
        assert (!envType.equals(EnvType.STANDARD_JDK)) && (!envType.equals(EnvType.INLAND_JDK)) :
            "Protocol using " + CommonConstants.MPC4J_NATIVE_FHE_NAME
                + " must not be " + EnvType.STANDARD_JDK.name() + " or " + EnvType.INLAND_JDK.name()
                + ": " + envType.name();
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
        return mpOprfConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }

    @Override
    public UpsiType getPtoType() {
        return UpsiType.CMG21;
    }

    public MpOprfConfig getMpOprfConfig() {
        return mpOprfConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cmg21UpsiConfig> {
        /**
         * MP-OPRF协议
         */
        private MpOprfConfig mpOprfConfig;

        public Builder() {
            mpOprfConfig = OprfFactory.createMpOprfDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setMpOprfConfig(MpOprfConfig mpOprfConfig) {
            this.mpOprfConfig = mpOprfConfig;
            return this;
        }

        @Override
        public Cmg21UpsiConfig build() {
            return new Cmg21UpsiConfig(this);
        }
    }
}
