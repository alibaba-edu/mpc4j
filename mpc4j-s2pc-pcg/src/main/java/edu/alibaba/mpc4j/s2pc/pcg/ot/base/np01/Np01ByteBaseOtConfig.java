package edu.alibaba.mpc4j.s2pc.pcg.ot.base.np01;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;

/**
 * NP01-字节基础OT协议配置项。
 *
 * @author Weiran Liu
 * @date 2023/4/24
 */
public class Np01ByteBaseOtConfig implements BaseOtConfig {
    /**
     * environment
     */
    private EnvType envType;

    private Np01ByteBaseOtConfig(Builder builder) {
        envType = EnvType.STANDARD;
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

    @Override
    public BaseOtFactory.BaseOtType getPtoType() {
        return BaseOtFactory.BaseOtType.NP01_BYTE;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Np01ByteBaseOtConfig> {

        public Builder() {
            // empty
        }

        @Override
        public Np01ByteBaseOtConfig build() {
            return new Np01ByteBaseOtConfig(this);
        }
    }
}
