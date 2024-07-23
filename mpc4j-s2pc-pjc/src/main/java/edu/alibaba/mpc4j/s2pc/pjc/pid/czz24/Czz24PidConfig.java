package edu.alibaba.mpc4j.s2pc.pjc.pid.czz24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidFactory.PidType;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;

/**
 * CZZ24 PID config.
 *
 * @author Yufei Wang
 * @date 2023/7/27
 */
public class Czz24PidConfig extends AbstractMultiPartyPtoConfig implements PidConfig {
    /**
     * OPRF
     */
    private final OprfConfig oprfConfig;
    /**
     * PSU
     */
    private final PsuConfig psuConfig;
    /**
     * Sloppy OKVS type
     */
    private final Gf2eDokvsType sloppyOkvsType;
    /**
     * cukoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;

    private Czz24PidConfig(Czz24PidConfig.Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.psuConfig, builder.oprfConfig);
        psuConfig = builder.psuConfig;
        oprfConfig = builder.oprfConfig;
        sloppyOkvsType = builder.sloppyOkvsType;
        cuckooHashBinType = builder.cuckooHashBinType;
    }

    @Override
    public PidType getPtoType() {
        return PidType.CZZ24;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Czz24PidConfig> {
        /**
         * OPRF
         */
        private OprfConfig oprfConfig;
        /**
         * PSU
         */
        private PsuConfig psuConfig;
        /**
         * Sloppy OKVS type
         */
        private Gf2eDokvsType sloppyOkvsType;
        /**
         * cuckoo hash bin type
         */
        private final CuckooHashBinType cuckooHashBinType;

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

        @Override
        public Czz24PidConfig build() {
            return new Czz24PidConfig(this);
        }
    }
}
