package edu.alibaba.mpc4j.s2pc.pir.index.xpir;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirFactory;

/**
 * XPIR协议配置项。
 *
 * @author Liqiang Peng
 * @date 2022/8/25
 */
public class Mbfk16IndexPirConfig implements IndexPirConfig {
    /**
     * 多项式阶
     */
    private final int polyModulusDegree;
    /**
     * 明文模数
     */
    private final int plainModulusSize;

    public Mbfk16IndexPirConfig(Builder builder) {
        this.polyModulusDegree = builder.polyModulusDegree;
        this.plainModulusSize = builder.plainModulusSize;
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
        return IndexPirFactory.IndexPirType.XPIR;
    }

    public int getPolyModulusDegree() {
        return polyModulusDegree;
    }

    public int getPlainModulusSize() {
        return plainModulusSize;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Mbfk16IndexPirConfig> {
        /**
         * 环境类型
         */
        private EnvType envType;
        /**
         * 多项式阶
         */
        private int polyModulusDegree;
        /**
         * 明文模数
         */
        private int plainModulusSize;

        public Builder() {
            envType = EnvType.STANDARD;
            polyModulusDegree = 4096;
            plainModulusSize = 20;
        }

        public Builder setEnvType(EnvType envType) {
            this.envType = envType;
            return this;
        }

        public Builder setPolyModulusDegree(int polyModulusDegree) {
            this.polyModulusDegree = polyModulusDegree;
            return this;
        }

        public Builder setPlainModulusSize(int plainModulusSize) {
            this.plainModulusSize = plainModulusSize;
            return this;
        }

        @Override
        public Mbfk16IndexPirConfig build() {
            return new Mbfk16IndexPirConfig(this);
        }
    }
}
