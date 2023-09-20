package edu.alibaba.mpc4j.s2pc.pir.index.batch.simplepir;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.single.simplepir.Hhcm23SimpleSingleIndexPirConfig;

/**
 * Batch Simple PIR based on Cuckoo Hash config.
 *
 * @author Liqiang Peng
 * @date 2023/7/7
 */
public class CuckooHashBatchSimplePirConfig extends AbstractMultiPartyPtoConfig implements BatchIndexPirConfig {
    /**
     * simple PIR config
     */
    private final Hhcm23SimpleSingleIndexPirConfig simplePirConfig;
    /**
     * cuckoo hash type
     */
    private final IntCuckooHashBinFactory.IntCuckooHashBinType cuckooHashBinType;
    /**
     * communication optimal
     */
    private final boolean communicationOptimal;

    public CuckooHashBatchSimplePirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        simplePirConfig = builder.simplePirConfig;
        cuckooHashBinType = builder.cuckooHashBinType;
        communicationOptimal = builder.communicationOptimal;
    }

    public Hhcm23SimpleSingleIndexPirConfig getSimplePirConfig() {
        return simplePirConfig;
    }

    public IntCuckooHashBinFactory.IntCuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    @Override
    public BatchIndexPirFactory.BatchIndexPirType getPtoType() {
        return BatchIndexPirFactory.BatchIndexPirType.SIMPLE_PIR;
    }

    public boolean isCommunicationOptimal() {
        return communicationOptimal;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<CuckooHashBatchSimplePirConfig> {
        /**
         * simple PIR config
         */
        private Hhcm23SimpleSingleIndexPirConfig simplePirConfig;
        /**
         * cuckoo hash
         */
        private IntCuckooHashBinFactory.IntCuckooHashBinType cuckooHashBinType;
        /**
         * communication optimal
         */
        private boolean communicationOptimal;

        public Builder() {
            simplePirConfig = new Hhcm23SimpleSingleIndexPirConfig.Builder().build();
            cuckooHashBinType = IntCuckooHashBinFactory.IntCuckooHashBinType.NO_STASH_NAIVE;
            communicationOptimal = true;
        }

        public Builder setSimplePirConfig(Hhcm23SimpleSingleIndexPirConfig simplePirConfig) {
            this.simplePirConfig = simplePirConfig;
            return this;
        }

        public Builder setCuckooHashBinType(IntCuckooHashBinFactory.IntCuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        public Builder setCommunicationOptimal(boolean communicationOptimal) {
            this.communicationOptimal = communicationOptimal;
            return this;
        }

        @Override
        public CuckooHashBatchSimplePirConfig build() {
            return new CuckooHashBatchSimplePirConfig(this);
        }
    }
}
