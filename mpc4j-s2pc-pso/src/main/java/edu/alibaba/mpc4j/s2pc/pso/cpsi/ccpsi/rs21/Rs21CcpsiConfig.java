package edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.rs21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2k.Gf2kDokvsFactory.Gf2kDokvsType;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.okvs.OkvsBopprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.rs21.Rs21MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.BopprfCcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory.CcpsiType;

/**
 * RS21 client-payload circuit PSI config.
 *
 * @author Weiran Liu
 * @date 2023/7/28
 */
public class Rs21CcpsiConfig extends AbstractMultiPartyPtoConfig implements BopprfCcpsiConfig {
    /**
     * Batch OPPRF config
     */
    private final BopprfConfig bopprfConfig;
    /**
     * private equality test config
     */
    private final PeqtConfig peqtConfig;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;

    private Rs21CcpsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.bopprfConfig, builder.peqtConfig);
        bopprfConfig = builder.bopprfConfig;
        peqtConfig = builder.peqtConfig;
        cuckooHashBinType = builder.cuckooHashBinType;
    }

    @Override
    public CcpsiType getPtoType() {
        return CcpsiType.RS21;
    }

    @Override
    public int getOutputBitNum(int serverElementSize, int clientElementSize) {
        return CuckooHashBinFactory.getBinNum(cuckooHashBinType, clientElementSize);
    }

    @Override
    public BopprfConfig getBopprfConfig() {
        return bopprfConfig;
    }

    @Override
    public PeqtConfig getPeqtConfig() {
        return peqtConfig;
    }

    @Override
    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rs21CcpsiConfig> {
        /**
         * Batch OPPRF config
         */
        private BopprfConfig bopprfConfig;
        /**
         * private equality test config
         */
        private PeqtConfig peqtConfig;
        /**
         * cuckoo hash bin type
         */
        private CuckooHashBinType cuckooHashBinType;

        public Builder(boolean silent) {
            bopprfConfig = new OkvsBopprfConfig.Builder()
                .setOprfConfig(new Rs21MpOprfConfig.Builder(SecurityModel.SEMI_HONEST).build())
                .build();
            peqtConfig = PeqtFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            cuckooHashBinType = CuckooHashBinType.NO_STASH_PSZ18_3_HASH;
        }

        public Builder setPeqtConfig(PeqtConfig peqtConfig) {
            this.peqtConfig = peqtConfig;
            return this;
        }

        public Builder setOkveType(Gf2kDokvsType okvsType){
            this.bopprfConfig = new OkvsBopprfConfig.Builder()
                .setOprfConfig(new Rs21MpOprfConfig.Builder(SecurityModel.SEMI_HONEST).setOkvsType(okvsType).build())
                .build();
            return this;
        }

        public Builder setCuckooHashBinType(CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        @Override
        public Rs21CcpsiConfig build() {
            return new Rs21CcpsiConfig(this);
        }
    }
}
