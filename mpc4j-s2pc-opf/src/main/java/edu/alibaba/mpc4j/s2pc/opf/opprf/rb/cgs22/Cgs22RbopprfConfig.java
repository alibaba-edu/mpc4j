package edu.alibaba.mpc4j.s2pc.opf.opprf.rb.cgs22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.s2pc.opf.opprf.rb.RbopprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.opprf.rb.RbopprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;

/**
 * CGS22 Related-Batch OPPRF config.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public class Cgs22RbopprfConfig extends AbstractMultiPartyPtoConfig implements RbopprfConfig {
    /**
     * d = 3
     */
    private static final int D = 3;
    /**
     * OPRF config
     */
    private final OprfConfig oprfConfig;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;

    private Cgs22RbopprfConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.oprfConfig);
        oprfConfig = builder.oprfConfig;
        cuckooHashBinType = builder.cuckooHashBinType;
    }

    @Override
    public RbopprfFactory.RbopprfType getPtoType() {
        return RbopprfFactory.RbopprfType.CGS22;
    }

    public OprfConfig getOprfConfig() {
        return oprfConfig;
    }

    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    @Override
    public int getD() {
        return D;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cgs22RbopprfConfig> {
        /**
         * OPRF config
         */
        private OprfConfig oprfConfig;
        /**
         * cuckoo hash bin type
         */
        private CuckooHashBinType cuckooHashBinType;

        public Builder() {
            oprfConfig = OprfFactory.createOprfDefaultConfig(SecurityModel.SEMI_HONEST);
            cuckooHashBinType = CuckooHashBinType.NO_STASH_PSZ18_3_HASH;
        }

        public Builder setOprfConfig(OprfConfig oprfConfig) {
            this.oprfConfig = oprfConfig;
            return this;
        }

        public Builder setCuckooHashBinType(CuckooHashBinType cuckooHashBinType) {
            int hashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
            MathPreconditions.checkEqual("hashNum", "D", hashNum, D);
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        @Override
        public Cgs22RbopprfConfig build() {
            return new Cgs22RbopprfConfig(this);
        }
    }
}
