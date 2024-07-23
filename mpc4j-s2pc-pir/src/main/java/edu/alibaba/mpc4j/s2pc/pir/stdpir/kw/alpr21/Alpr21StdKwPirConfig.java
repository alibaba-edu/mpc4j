package edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.alpr21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.PbcableStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal.SealStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.StdKwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.StdKwPirFactory;

import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;

/**
 * ALPR21 standard keyword PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/7/4
 */
public class Alpr21StdKwPirConfig extends AbstractMultiPartyPtoConfig implements StdKwPirConfig {
    /**
     * probabilistic batch code (PBC) index PIR config
     */
    private final PbcableStdIdxPirConfig pbcableStdIdxPirConfig;
    /**
     * cuckoo hash type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * params
     */
    private final Alpr21StdKwPirParams params;

    public Alpr21StdKwPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.pbcableStdIdxPirConfig);
        pbcableStdIdxPirConfig = builder.pbcableStdIdxPirConfig;
        cuckooHashBinType = builder.cuckooHashBinType;
        params = builder.params;
    }

    public PbcableStdIdxPirConfig getPbcableStdIdxPirConfig() {
        return pbcableStdIdxPirConfig;
    }

    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    @Override
    public StdKwPirFactory.StdKwPirType getPtoType() {
        return StdKwPirFactory.StdKwPirType.ALPR21;
    }

    public Alpr21StdKwPirParams getParams() {
        return params;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Alpr21StdKwPirConfig> {
        /**
         * probabilistic batch code (PBC) index PIR config
         */
        private PbcableStdIdxPirConfig pbcableStdIdxPirConfig;
        /**
         * cuckoo hash
         */
        private CuckooHashBinType cuckooHashBinType;
        /**
         * params
         */
        private Alpr21StdKwPirParams params;

        public Builder() {
            pbcableStdIdxPirConfig = new SealStdIdxPirConfig.Builder().build();
            cuckooHashBinType = CuckooHashBinType.NO_STASH_NAIVE;
            params = Alpr21StdKwPirParams.DEFAULT_PARAMS;
        }

        public Builder setPbcableStdIdxPirConfig(PbcableStdIdxPirConfig pbcableStdIdxPirConfig) {
            this.pbcableStdIdxPirConfig = pbcableStdIdxPirConfig;
            return this;
        }

        public Builder setCuckooHashBinType(CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        public Builder setParams(Alpr21StdKwPirParams params) {
            this.params = params;
            return this;
        }

        @Override
        public Alpr21StdKwPirConfig build() {
            return new Alpr21StdKwPirConfig(this);
        }
    }
}
