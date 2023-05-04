package edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirFactory;

/**
 * PSI-PIR协议配置项。
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class Mr23BatchIndexPirConfig implements BatchIndexPirConfig {
    /**
     * 是否使用压缩椭圆曲线编码
     */
    private final boolean compressEncode;

    public Mr23BatchIndexPirConfig(Builder builder) {
        compressEncode = builder.compressEncode;
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

    public boolean getCompressEncode() {
        return compressEncode;
    }

    @Override
    public BatchIndexPirFactory.BatchIndexPirType getPtoType() {
        return BatchIndexPirFactory.BatchIndexPirType.VECTORIZED_BATCH_PIR;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Mr23BatchIndexPirConfig> {
        /**
         * 是否使用压缩椭圆曲线编码
         */
        private boolean compressEncode;

        public Builder() {
            compressEncode = true;
        }

        public Builder setCompressEncode(boolean compressEncode) {
            this.compressEncode = compressEncode;
            return this;
        }

        @Override
        public Mr23BatchIndexPirConfig build() {
            return new Mr23BatchIndexPirConfig(this);
        }
    }
}
