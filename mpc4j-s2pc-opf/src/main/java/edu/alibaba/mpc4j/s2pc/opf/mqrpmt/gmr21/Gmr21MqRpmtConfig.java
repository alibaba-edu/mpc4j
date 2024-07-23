package edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnFactory;

/**
 * GMR21-mqRPMT config.
 *
 * @author Weiran Liu
 * @date 2022/09/10
 */
public class Gmr21MqRpmtConfig extends AbstractMultiPartyPtoConfig implements MqRpmtConfig {
    /**
     * OPRF used in cuckoo hash
     */
    private final OprfConfig cuckooHashOprfConfig;
    /**
     * OPRF used in PEQT
     */
    private final OprfConfig peqtOprfConfig;
    /**
     * OSN
     */
    private final DosnConfig dosnConfig;
    /**
     * OKVS type
     */
    private final Gf2eDokvsType okvsType;
    /**
     * cuckoo hash type
     */
    private final CuckooHashBinType cuckooHashBinType;

    private Gmr21MqRpmtConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.cuckooHashOprfConfig, builder.peqtOprfConfig, builder.dosnConfig);
        cuckooHashOprfConfig = builder.cuckooHashOprfConfig;
        peqtOprfConfig = builder.peqtOprfConfig;
        dosnConfig = builder.dosnConfig;
        okvsType = builder.okvsType;
        cuckooHashBinType = builder.cuckooHashBinType;
    }

    @Override
    public MqRpmtFactory.MqRpmtType getPtoType() {
        return MqRpmtFactory.MqRpmtType.GMR21;
    }

    @Override
    public int getVectorLength(int serverElementSize, int clientElementSize) {
        MathPreconditions.checkGreater("server_element_size", serverElementSize, 1);
        MathPreconditions.checkGreater("client_element_size", clientElementSize, 1);
        return CuckooHashBinFactory.getBinNum(cuckooHashBinType, serverElementSize);
    }

    public OprfConfig getCuckooHashOprfConfig() {
        return cuckooHashOprfConfig;
    }

    public OprfConfig getPeqtOprfConfig() {
        return peqtOprfConfig;
    }

    public DosnConfig getOsnConfig() {
        return dosnConfig;
    }

    public Gf2eDokvsType getOkvsType() {
        return okvsType;
    }

    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Gmr21MqRpmtConfig> {
        /**
         * OPRF used in cuckoo hash
         */
        private final OprfConfig cuckooHashOprfConfig;
        /**
         * OPRF used in PEQT
         */
        private final OprfConfig peqtOprfConfig;
        /**
         * OSN
         */
        private final DosnConfig dosnConfig;
        /**
         * OKVS type
         */
        private Gf2eDokvsType okvsType;
        /**
         * cuckoo hash type
         */
        private CuckooHashBinType cuckooHashBinType;

        public Builder(boolean silent) {
            cuckooHashOprfConfig = OprfFactory.createOprfDefaultConfig(SecurityModel.SEMI_HONEST);
            peqtOprfConfig = OprfFactory.createOprfDefaultConfig(SecurityModel.SEMI_HONEST);
            dosnConfig = DosnFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            okvsType = Gf2eDokvsType.MEGA_BIN;
            // this type is used in the GMR21 implementation
            // see https://github.com/osu-crypto/PSI-analytics/blob/master/test/psi_analytics_eurocrypt19_test.cpp#L35
            cuckooHashBinType = CuckooHashBinType.NAIVE_3_HASH;
        }

        public Builder setOkvsType(Gf2eDokvsType okvsType) {
            this.okvsType = okvsType;
            return this;
        }

        public Builder setCuckooHashBinType(CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        @Override
        public Gmr21MqRpmtConfig build() {
            return new Gmr21MqRpmtConfig(this);
        }
    }
}
