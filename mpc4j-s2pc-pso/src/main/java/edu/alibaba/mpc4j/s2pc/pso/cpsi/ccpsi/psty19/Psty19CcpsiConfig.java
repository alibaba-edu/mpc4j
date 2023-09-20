package edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.psty19;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.BopprfCcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory.CcpsiType;

/**
 * PSTY19 client-payload circuit PSI config.
 *
 * @author Weiran Liu
 * @date 2023/4/19
 */
public class Psty19CcpsiConfig extends AbstractMultiPartyPtoConfig implements BopprfCcpsiConfig {
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

    private Psty19CcpsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.bopprfConfig, builder.peqtConfig);
        bopprfConfig = builder.bopprfConfig;
        peqtConfig = builder.peqtConfig;
        cuckooHashBinType = builder.cuckooHashBinType;
    }

    @Override
    public CcpsiType getPtoType() {
        return CcpsiType.PSTY19;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Psty19CcpsiConfig> {
        /**
         * Batch OPPRF config
         */
        private final BopprfConfig bopprfConfig;
        /**
         * private equality test config
         */
        private PeqtConfig peqtConfig;
        /**
         * cuckoo hash bin type
         */
        private CuckooHashBinType cuckooHashBinType;

        public Builder(boolean silent) {
            bopprfConfig = BopprfFactory.createDefaultConfig();
            peqtConfig = PeqtFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            cuckooHashBinType = CuckooHashBinType.NO_STASH_PSZ18_3_HASH;
        }

        public Builder setPeqtConfig(PeqtConfig peqtConfig) {
            this.peqtConfig = peqtConfig;
            return this;
        }

        public Builder setCuckooHashBinType(CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        @Override
        public Psty19CcpsiConfig build() {
            return new Psty19CcpsiConfig(this);
        }
    }
}
