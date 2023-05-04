package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.bea95;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.PreLnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.PreLnotFactory;

/**
 * @author Weiran Liu
 * @date 2023/4/11
 */
public class Bea95PreLnotConfig implements PreLnotConfig {
    /**
     * the environment
     */
    private EnvType envType;

    private Bea95PreLnotConfig(Builder builder) {
        envType = EnvType.STANDARD;
    }

    @Override
    public PreLnotFactory.PreLnotType getPtoType() {
        return PreLnotFactory.PreLnotType.Bea95;
    }

    @Override
    public void setEnvType(EnvType envType) {
        this.envType = envType;
    }

    @Override
    public EnvType getEnvType() {
        return envType;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.MALICIOUS;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Bea95PreLnotConfig> {

        public Builder() {
            // empty
        }

        @Override
        public Bea95PreLnotConfig build() {
            return new Bea95PreLnotConfig(this);
        }
    }
}
