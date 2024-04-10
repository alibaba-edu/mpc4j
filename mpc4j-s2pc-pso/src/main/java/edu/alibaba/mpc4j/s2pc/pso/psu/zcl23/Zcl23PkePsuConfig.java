package edu.alibaba.mpc4j.s2pc.pso.psu.zcl23;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc.EccDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc.EccDokvsFactory.EccDokvsType;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.zp.ZpDokvsFactory.ZpDokvsType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory.PsuType;

/**
 * ZCL23-PKE-PSU protocol config.
 *
 * @author Weiran Liu
 * @date 2022/02/16
 */
public class Zcl23PkePsuConfig extends AbstractMultiPartyPtoConfig implements PsuConfig {
    /**
     * 核COT协议配置项
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * Zp-DOKVS type
     */
    private final ZpDokvsType zpDokvsType;
    /**
     * ECC-DOKVS type
     */
    private final EccDokvsType eccDokvsType;
    /**
     * 是否使用压缩椭圆曲线编码
     */
    private final boolean compressEncode;
    /**
     * 流水线数量
     */
    private final int pipeSize;

    private Zcl23PkePsuConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.coreCotConfig);
        coreCotConfig = builder.coreCotConfig;
        eccDokvsType = builder.eccDokvsType;
        zpDokvsType = EccDokvsFactory.getCorrespondingEccDokvsType(eccDokvsType);
        compressEncode = builder.compressEncode;
        pipeSize = builder.pipeSize;
    }

    @Override
    public PsuType getPtoType() {
        return PsuType.ZCL23_PKE;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public ZpDokvsType getZpDokvsType() {
        return zpDokvsType;
    }

    public EccDokvsType getEccDokvsType() {
        return eccDokvsType;
    }

    public boolean getCompressEncode() {
        return compressEncode;
    }

    public int getPipeSize() {
        return pipeSize;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Zcl23PkePsuConfig> {
        /**
         * 核COT协议配置项
         */
        private CoreCotConfig coreCotConfig;
        /**
         * ECC-DOKVS type
         */
        private EccDokvsType eccDokvsType;
        /**
         * 是否使用压缩椭圆曲线编码
         */
        private boolean compressEncode;
        /**
         * 流水线数量
         */
        private int pipeSize;

        public Builder() {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            eccDokvsType = EccDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT;
            compressEncode = true;
            pipeSize = (1 << 8);
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        public Builder setEccDokvsType(EccDokvsType eccDokvsType) {
            this.eccDokvsType = eccDokvsType;
            return this;
        }

        public Builder setCompressEncode(boolean compressEncode) {
            this.compressEncode = compressEncode;
            return this;
        }

        public Builder setPipeSize(int pipeSize) {
            MathPreconditions.checkPositive("pipeSize", pipeSize);
            this.pipeSize = pipeSize;
            return this;
        }

        @Override
        public Zcl23PkePsuConfig build() {
            return new Zcl23PkePsuConfig(this);
        }
    }
}
