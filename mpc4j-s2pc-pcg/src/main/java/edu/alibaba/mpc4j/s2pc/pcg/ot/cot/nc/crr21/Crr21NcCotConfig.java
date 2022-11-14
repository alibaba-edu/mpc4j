package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.crr21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;

import edu.alibaba.mpc4j.common.tool.lpn.ldpc.LdpcCreatorUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.bcg19.Bcg19RegMspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;

/**
 * CRR21-NC-COT协议配置项。
 *
 * @author Hanwen Feng
 * @date 2022/02/18
 */

public class Crr21NcCotConfig implements NcCotConfig {
    /**
     * MSP-COT协议配置项
     */
    private final MspCotConfig mspCotConfig;
    /**
     * LDPC类型
     */
    private final LdpcCreatorUtils.CodeType codeType;

    private Crr21NcCotConfig(Builder builder) {
        mspCotConfig = builder.mspcotConfig;
        codeType = builder.codeType;
    }

    public MspCotConfig getMspCotConfig() {
        return mspCotConfig;
    }

    @Override
    public NcCotFactory.NcCotType getPtoType() {
        return NcCotFactory.NcCotType.CRR21;
    }

    @Override
    public int maxAllowNum() {
        return 1 << Crr21NcCotPtoDesc.MAX_LOG_N;
    }

    @Override
    public void setEnvType(EnvType envType) {
        mspCotConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return mspCotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        return mspCotConfig.getSecurityModel();
    }

    public LdpcCreatorUtils.CodeType getCodeType() {
        return codeType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Crr21NcCotConfig> {
        /**
         * MSP-COT协议配置项
         */
        private MspCotConfig mspcotConfig;
        /**
         * LDPC类型
         */
        private LdpcCreatorUtils.CodeType codeType;

        public Builder() {
            mspcotConfig = new Bcg19RegMspCotConfig.Builder(SecurityModel.SEMI_HONEST).build();
            codeType = LdpcCreatorUtils.CodeType.SILVER_5;
        }

        public Builder setMspCotConfig(MspCotConfig mspcotConfig) {
            this.mspcotConfig = mspcotConfig;
            return this;
        }
        public Builder setCodeType(LdpcCreatorUtils.CodeType codeType) {
            this.codeType = codeType;
            return this;
        }

        @Override
        public Crr21NcCotConfig build() {
            return new Crr21NcCotConfig(this);
        }
    }
}
