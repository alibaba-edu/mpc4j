package edu.alibaba.mpc4j.s2pc.pir.keyword.alpr21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.simplepir.CuckooHashBatchSimplePirConfig;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirFactory;

/**
 * ALPR21 keyword PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/7/4
 */
public class Alpr21KwPirConfig extends AbstractMultiPartyPtoConfig implements KwPirConfig {
    /**
     * batch index PIR config
     */
    private final BatchIndexPirConfig indexPirConfig;
    /**
     * cuckoo hash type
     */
    private final CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType;

    public Alpr21KwPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.indexPirConfig);
        indexPirConfig = builder.indexPirConfig;
        cuckooHashBinType = builder.cuckooHashBinType;
    }

    @Override
    public KwPirFactory.KwPirType getProType() {
        return KwPirFactory.KwPirType.ALPR21;
    }

    public BatchIndexPirConfig getBatchIndexPirConfig() {
        return indexPirConfig;
    }

    public CuckooHashBinFactory.CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Alpr21KwPirConfig> {
        /**
         * batch index PIR config
         */
        private BatchIndexPirConfig indexPirConfig;
        /**
         * cuckoo hash
         */
        private CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType;

        public Builder() {
            indexPirConfig = new CuckooHashBatchSimplePirConfig.Builder().build();
            cuckooHashBinType = CuckooHashBinFactory.CuckooHashBinType.NO_STASH_NAIVE;
        }

        public Builder setBatchIndexPirConfig(BatchIndexPirConfig indexPirConfig) {
            this.indexPirConfig = indexPirConfig;
            return this;
        }

        public Builder setCuckooHashBinType(CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        @Override
        public Alpr21KwPirConfig build() {
            return new Alpr21KwPirConfig(this);
        }
    }
}
