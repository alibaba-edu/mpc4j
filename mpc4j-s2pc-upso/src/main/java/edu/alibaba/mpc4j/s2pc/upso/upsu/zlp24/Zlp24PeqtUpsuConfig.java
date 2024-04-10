package edu.alibaba.mpc4j.s2pc.upso.upsu.zlp24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.PmPeqtConfig;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.PmPeqtFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.upso.upsu.UpsuConfig;
import edu.alibaba.mpc4j.s2pc.upso.upsu.UpsuFactory;

/**
 * ZLP24 UPSU config.
 *
 * @author Liqiang Peng
 * @date 2024/3/20
 */
public class Zlp24PeqtUpsuConfig extends AbstractMultiPartyPtoConfig implements UpsuConfig {
    /**
     * single - query config
     */
    private final SqOprfConfig sqOprfConfig;
    /**
     * permute matrix PEQT config
     */
    private final PmPeqtConfig pmPeqtConfig;
    /**
     * batch index PIR config
     */
    private final BatchIndexPirConfig batchIndexPirConfig;
    /**
     * core COT config
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * cuckoo hash type
     */
    private final CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType;
    /**
     * Gf2e dokvs type
     */
    private final Gf2eDokvsFactory.Gf2eDokvsType gf2eDokvsType;

    public Zlp24PeqtUpsuConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.sqOprfConfig, builder.pmPeqtConfig, builder.batchIndexPirConfig,
            builder.coreCotConfig);
        sqOprfConfig = builder.sqOprfConfig;
        pmPeqtConfig = builder.pmPeqtConfig;
        batchIndexPirConfig = builder.batchIndexPirConfig;
        coreCotConfig = builder.coreCotConfig;
        cuckooHashBinType = builder.cuckooHashBinType;
        gf2eDokvsType = builder.gf2eDokvsType;
    }

    @Override
    public UpsuFactory.UpsuType getPtoType() {
        return UpsuFactory.UpsuType.ZLP24_PEQT;
    }

    public SqOprfConfig getSqOprfConfig() {
        return sqOprfConfig;
    }

    public PmPeqtConfig getPmPeqtConfig() {
        return pmPeqtConfig;
    }

    public BatchIndexPirConfig getBatchIndexPirConfig() {
        return batchIndexPirConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public CuckooHashBinFactory.CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    public Gf2eDokvsFactory.Gf2eDokvsType getGf2eDokvsType() {
        return gf2eDokvsType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Zlp24PeqtUpsuConfig> {
        /**
         * single - query config
         */
        private SqOprfConfig sqOprfConfig;
        /**
         * permute matrix PEQT config
         */
        private PmPeqtConfig pmPeqtConfig;
        /**
         * batch index PIR config
         */
        private BatchIndexPirConfig batchIndexPirConfig;
        /**
         * core COT config
         */
        private CoreCotConfig coreCotConfig;
        /**
         * cuckoo hash type
         */
        private CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType;
        /**
         * Gf2e dokvs type
         */
        private Gf2eDokvsFactory.Gf2eDokvsType gf2eDokvsType;

        public Builder() {
            sqOprfConfig = SqOprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            pmPeqtConfig = PmPeqtFactory.createPmPeqtDefaultConfig(SecurityModel.SEMI_HONEST);
            batchIndexPirConfig = BatchIndexPirFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            cuckooHashBinType = CuckooHashBinFactory.CuckooHashBinType.NO_STASH_PSZ18_3_HASH;
            gf2eDokvsType = Gf2eDokvsFactory.Gf2eDokvsType.H3_SPARSE_CLUSTER_BLAZE_GCT;
        }

        public Builder setSqOprfConfig(SqOprfConfig sqOprfConfig) {
            this.sqOprfConfig = sqOprfConfig;
            return this;
        }

        public Builder setPmPeqtConfig(PmPeqtConfig pmPeqtConfig) {
            this.pmPeqtConfig = pmPeqtConfig;
            return this;
        }

        public Builder setBatchIndexPirConfig(BatchIndexPirConfig batchIndexPirConfig) {
            this.batchIndexPirConfig = batchIndexPirConfig;
            return this;
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        public Builder setCuckooHashBinType(CuckooHashBinFactory.CuckooHashBinType  cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        public Builder setGf2eDokvsType(Gf2eDokvsFactory.Gf2eDokvsType gf2eDokvsType) {
            this.gf2eDokvsType = gf2eDokvsType;
            return this;
        }

        @Override
        public Zlp24PeqtUpsuConfig build() {
            return new Zlp24PeqtUpsuConfig(this);
        }
    }
}
