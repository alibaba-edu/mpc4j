package edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.pir;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir.Mr23BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.UrbopprfConfig;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.UrbopprfFactory;

/**
 * sparse unbalanced related-batch OPPRF config.
 *
 * @author Liqiang Peng
 * @date 2023/4/20
 */
public class PirUrbopprfConfig implements UrbopprfConfig {
    /**
     * d = 3
     */
    private static final int D = 3;
    /**
     * single-query OPRF config
     */
    private final SqOprfConfig sqOprfConfig;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * batch index PIR config
     */
    private final BatchIndexPirConfig batchIndexPirConfig;

    private PirUrbopprfConfig(Builder builder) {
        sqOprfConfig = builder.sqOprfConfig;
        cuckooHashBinType = builder.cuckooHashBinType;
        batchIndexPirConfig = builder.batchIndexPirConfig;
    }

    @Override
    public UrbopprfFactory.UrbopprfType getPtoType() {
        return UrbopprfFactory.UrbopprfType.PIR;
    }

    @Override
    public void setEnvType(EnvType envType) {
        sqOprfConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return sqOprfConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }

    public SqOprfConfig getSqOprfConfig() {
        return sqOprfConfig;
    }

    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    public BatchIndexPirConfig getBatchIndexPirConfig() {
        return batchIndexPirConfig;
    }

    @Override
    public int getD() {
        return D;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<PirUrbopprfConfig> {
        /**
         * OPRF config
         */
        private SqOprfConfig sqOprfConfig;
        /**
         * cuckoo hash bin type
         */
        private CuckooHashBinType cuckooHashBinType;
        /**
         * batch index PIR config
         */
        private BatchIndexPirConfig batchIndexPirConfig;

        public Builder() {
            sqOprfConfig = SqOprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            cuckooHashBinType = CuckooHashBinType.NO_STASH_PSZ18_3_HASH;
            batchIndexPirConfig = new Mr23BatchIndexPirConfig.Builder().build();
        }

        public Builder setSqOprfConfig(SqOprfConfig sqOprfConfig) {
            this.sqOprfConfig = sqOprfConfig;
            return this;
        }

        public Builder setCuckooHashBinType(CuckooHashBinType cuckooHashBinType) {
            int hashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
            MathPreconditions.checkEqual("hashNum", "D", hashNum, D);
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        public Builder setBatchIndexPirConfig(BatchIndexPirConfig batchIndexPirConfig) {
            this.batchIndexPirConfig = batchIndexPirConfig;
            return this;
        }

        @Override
        public PirUrbopprfConfig build() {
            return new PirUrbopprfConfig(this);
        }
    }
}
