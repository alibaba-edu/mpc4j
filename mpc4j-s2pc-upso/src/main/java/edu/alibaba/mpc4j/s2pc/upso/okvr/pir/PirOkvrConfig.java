package edu.alibaba.mpc4j.s2pc.upso.okvr.pir;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.vectorizedpir.Mr23BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.upso.okvr.OkvrConfig;
import edu.alibaba.mpc4j.s2pc.upso.okvr.OkvrFactory.OkvrType;

/**
 * PIR OKVR config.
 *
 * @author Liqiang Peng
 * @date 2023/4/20
 */
public class PirOkvrConfig extends AbstractMultiPartyPtoConfig implements OkvrConfig {
    /**
     * single-query OPRF config
     */
    private final SqOprfConfig sqOprfConfig;
    /**
     * OKVS type
     */
    private final Gf2eDokvsType okvsType;
    /**
     * batch index PIR config
     */
    private final BatchIndexPirConfig batchIndexPirConfig;

    private PirOkvrConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.sqOprfConfig, builder.batchIndexPirConfig);
        sqOprfConfig = builder.sqOprfConfig;
        okvsType = builder.okvsType;
        batchIndexPirConfig = builder.batchIndexPirConfig;
    }

    @Override
    public OkvrType getPtoType() {
        return OkvrType.PIR;
    }

    public SqOprfConfig getSqOprfConfig() {
        return sqOprfConfig;
    }

    public Gf2eDokvsType getOkvsType() {
        return okvsType;
    }

    public BatchIndexPirConfig getBatchIndexPirConfig() {
        return batchIndexPirConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<PirOkvrConfig> {
        /**
         * single-point OPRF config
         */
        private SqOprfConfig sqOprfConfig;
        /**
         * DOKVS type
         */
        private Gf2eDokvsType okvsType;
        /**
         * batch index PIR config
         */
        private BatchIndexPirConfig batchIndexPirConfig;

        public Builder() {
            sqOprfConfig = SqOprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            okvsType = Gf2eDokvsType.H2_SPARSE_CLUSTER_BLAZE_GCT;
            assert Gf2eDokvsFactory.isSparse(okvsType);
            batchIndexPirConfig = new Mr23BatchIndexPirConfig.Builder().build();
        }

        public Builder setSqOprfConfig(SqOprfConfig sqOprfConfig) {
            this.sqOprfConfig = sqOprfConfig;
            return this;
        }

        public Builder setSparseOkvsType(Gf2eDokvsType okvsType) {
            Preconditions.checkArgument(Gf2eDokvsFactory.isSparse(okvsType));
            this.okvsType = okvsType;
            return this;
        }

        public Builder setBatchIndexPirConfig(BatchIndexPirConfig batchIndexPirConfig) {
            this.batchIndexPirConfig = batchIndexPirConfig;
            return this;
        }

        @Override
        public PirOkvrConfig build() {
            return new PirOkvrConfig(this);
        }
    }
}
