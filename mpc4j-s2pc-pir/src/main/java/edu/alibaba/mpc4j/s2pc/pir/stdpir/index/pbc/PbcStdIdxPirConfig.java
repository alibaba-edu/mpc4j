package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.pbc;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory.IntCuckooHashBinType;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.PbcableStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal.SealStdIdxPirConfig;

/**
 * probabilistic batch code (PBC) batch index PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class PbcStdIdxPirConfig extends AbstractMultiPartyPtoConfig implements StdIdxPirConfig {
    /**
     * probabilistic batch code (PBC) index PIR config
     */
    private final PbcableStdIdxPirConfig pbcableStdIdxPirConfig;
    /**
     * cuckoo hash type
     */
    private final IntCuckooHashBinType cuckooHashBinType;

    public PbcStdIdxPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.pbcableStdIdxPirConfig);
        pbcableStdIdxPirConfig = builder.pbcableStdIdxPirConfig;
        cuckooHashBinType = builder.cuckooHashBinType;
    }

    public PbcableStdIdxPirConfig getPbcStdIdxPirConfig() {
        return pbcableStdIdxPirConfig;
    }

    public IntCuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    @Override
    public StdIdxPirFactory.StdIdxPirType getProType() {
        return StdIdxPirFactory.StdIdxPirType.PBC;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<PbcStdIdxPirConfig> {
        /**
         * probabilistic batch code (PBC) index PIR config
         */
        private PbcableStdIdxPirConfig pbcableStdIdxPirConfig;
        /**
         * cuckoo hash
         */
        private IntCuckooHashBinType cuckooHashBinType;

        public Builder() {
            pbcableStdIdxPirConfig = new SealStdIdxPirConfig.Builder().build();
            cuckooHashBinType = IntCuckooHashBinType.NO_STASH_NAIVE;
        }

        public Builder setPbcStdIdxPirConfig(PbcableStdIdxPirConfig pbcableStdIdxPirConfig) {
            this.pbcableStdIdxPirConfig = pbcableStdIdxPirConfig;
            return this;
        }

        public Builder setCuckooHashBinType(IntCuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        @Override
        public PbcStdIdxPirConfig build() {
            return new PbcStdIdxPirConfig(this);
        }
    }
}
