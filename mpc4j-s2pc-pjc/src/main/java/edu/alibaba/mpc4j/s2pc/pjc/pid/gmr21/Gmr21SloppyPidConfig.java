package edu.alibaba.mpc4j.s2pc.pjc.pid.gmr21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidFactory;

/**
 * GMR21宽松PID协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/5/12
 */
public class Gmr21SloppyPidConfig extends AbstractMultiPartyPtoConfig implements PidConfig {
    /**
     * OPRF协议配置项
     */
    private final OprfConfig oprfConfig;
    /**
     * PSU协议配置项
     */
    private final PsuConfig psuConfig;
    /**
     * Sloppy OKVS type
     */
    private final Gf2eDokvsType sloppyOkvsType;
    /**
     * 布谷鸟哈希类型
     */
    private final CuckooHashBinType cuckooHashBinType;

    private Gmr21SloppyPidConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.psuConfig, builder.oprfConfig);
        psuConfig = builder.psuConfig;
        oprfConfig = builder.oprfConfig;
        sloppyOkvsType = builder.sloppyOkvsType;
        cuckooHashBinType = builder.cuckooHashBinType;
    }

    @Override
    public PidFactory.PidType getPtoType() {
        return PidFactory.PidType.GMR21_SLOPPY;
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

    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Gmr21SloppyPidConfig> {
        /**
         * OPRF协议配置项
         */
        private OprfConfig oprfConfig;
        /**
         * PSU协议配置项
         */
        private PsuConfig psuConfig;
        /**
         * OKVS类型
         */
        private Gf2eDokvsType sloppyOkvsType;
        /**
         * 布谷鸟哈希类型
         */
        private CuckooHashBinType cuckooHashBinType;

        public Builder() {
            oprfConfig = OprfFactory.createOprfDefaultConfig(SecurityModel.SEMI_HONEST);
            psuConfig = PsuFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            sloppyOkvsType = Gf2eDokvsType.MEGA_BIN;
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

        public Builder setCuckooHashBinType(CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        @Override
        public Gmr21SloppyPidConfig build() {
            return new Gmr21SloppyPidConfig(this);
        }
    }
}
