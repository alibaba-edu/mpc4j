package edu.alibaba.mpc4j.s2pc.pjc.pmid.zcl22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.PmidConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.PmidFactory;

/**
 * ZCL22宽松PMID协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/5/14
 */
public class Zcl22SloppyPmidConfig extends AbstractMultiPartyPtoConfig implements PmidConfig {
    /**
     * OPRF协议配置项
     */
    private final OprfConfig oprfConfig;
    /**
     * PSU协议配置项
     */
    private final PsuConfig psuConfig;
    /**
     * Sloppy的OKVS类型
     */
    private final Gf2eDokvsType sloppyOkvsType;
    /**
     * σ的OKVS类型
     */
    private final Gf2eDokvsType sigmaOkvsType;
    /**
     * 布谷鸟哈希类型
     */
    private final CuckooHashBinType cuckooHashBinType;

    private Zcl22SloppyPmidConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.psuConfig, builder.oprfConfig);
        psuConfig = builder.psuConfig;
        oprfConfig = builder.oprfConfig;
        sloppyOkvsType = builder.sloppyOkvsType;
        sigmaOkvsType = builder.sigmaOkvsType;
        cuckooHashBinType = builder.cuckooHashBinType;
    }

    @Override
    public PmidFactory.PmidType getPtoType() {
        return PmidFactory.PmidType.ZCL22_SLOPPY;
    }

    public OprfConfig getOprfConfig() {
        return oprfConfig;
    }

    public PsuConfig getPsuConfig() {
        return psuConfig;
    }

    public Gf2eDokvsType getSloppyOkvsType() {
        return sloppyOkvsType;
    }

    public Gf2eDokvsType getSigmaOkvsType() {
        return sigmaOkvsType;
    }

    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Zcl22SloppyPmidConfig> {
        /**
         * OPRF协议配置项
         */
        private OprfConfig oprfConfig;
        /**
         * PSU协议配置项
         */
        private PsuConfig psuConfig;
        /**
         * Sloppy的OKVS类型
         */
        private Gf2eDokvsType sloppyOkvsType;
        /**
         * σ的OKVS类型
         */
        private Gf2eDokvsType sigmaOkvsType;
        /**
         * 布谷鸟哈希类型
         */
        private CuckooHashBinType cuckooHashBinType;

        public Builder() {
            oprfConfig = OprfFactory.createOprfDefaultConfig(SecurityModel.SEMI_HONEST);
            psuConfig = PsuFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            sloppyOkvsType = Gf2eDokvsType.MEGA_BIN;
            sigmaOkvsType = Gf2eDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT;
            cuckooHashBinType = CuckooHashBinType.NAIVE_3_HASH;
        }

        public Builder setOprfConfig(OprfConfig oprfConfig) {
            this.oprfConfig = oprfConfig;
            return this;
        }

        public Builder setPsuConfig(PsuConfig psuConfig) {
            this.psuConfig = psuConfig;
            return this;
        }

        public Builder setSloppyOkvsType(Gf2eDokvsType sloppyOkvsType) {
            this.sloppyOkvsType = sloppyOkvsType;
            return this;
        }

        public Builder setSigmaOkvsType(Gf2eDokvsType sigmaOkvsType) {
            this.sigmaOkvsType = sigmaOkvsType;
            return this;
        }

        public Builder setCuckooHashBinType(CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        @Override
        public Zcl22SloppyPmidConfig build() {
            return new Zcl22SloppyPmidConfig(this);
        }
    }
}
