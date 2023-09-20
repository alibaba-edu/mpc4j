package edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.cgs22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.s2pc.opf.opprf.rb.RbopprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.opprf.rb.RbopprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.PdsmConfig;
import edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.PdsmFactory;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory;

/**
 * CGS22 server-payload circuit PSI config.
 *
 * @author Weiran Liu
 * @date 2023/4/19
 */
public class Cgs22CcpsiConfig extends AbstractMultiPartyPtoConfig implements CcpsiConfig {
    /**
     * related batch OPPRF config
     */
    private final RbopprfConfig rbopprfConfig;
    /**
     * private set membership config
     */
    private final PdsmConfig pdsmConfig;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;

    private Cgs22CcpsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.rbopprfConfig, builder.pdsmConfig);
        rbopprfConfig = builder.rbopprfConfig;
        pdsmConfig = builder.pdsmConfig;
        cuckooHashBinType = builder.cuckooHashBinType;
    }

    @Override
    public CcpsiFactory.CcpsiType getPtoType() {
        return CcpsiFactory.CcpsiType.CGS22;
    }

    @Override
    public int getOutputBitNum(int serverElementSize, int clientElementSize) {
        return CuckooHashBinFactory.getBinNum(cuckooHashBinType, clientElementSize);
    }

    public RbopprfConfig getRbopprfConfig() {
        return rbopprfConfig;
    }

    public PdsmConfig getPsmConfig() {
        return pdsmConfig;
    }

    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cgs22CcpsiConfig> {
        /**
         * related batch OPPRF config
         */
        private RbopprfConfig rbopprfConfig;
        /**
         * private set membership config
         */
        private PdsmConfig pdsmConfig;
        /**
         * cuckoo hash bin type
         */
        private CuckooHashBinType cuckooHashBinType;

        public Builder(boolean silent) {
            rbopprfConfig = RbopprfFactory.createDefaultConfig();
            pdsmConfig = PdsmFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            cuckooHashBinType = CuckooHashBinType.NO_STASH_PSZ18_3_HASH;
        }

        public Builder setRbopprfConfig(RbopprfConfig rbopprfConfig) {
            this.rbopprfConfig = rbopprfConfig;
            return this;
        }

        public Builder setPsmConfig(PdsmConfig pdsmConfig) {
            this.pdsmConfig = pdsmConfig;
            return this;
        }

        public Builder setCuckooHashBinType(CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        @Override
        public Cgs22CcpsiConfig build() {
            return new Cgs22CcpsiConfig(this);
        }
    }
}
