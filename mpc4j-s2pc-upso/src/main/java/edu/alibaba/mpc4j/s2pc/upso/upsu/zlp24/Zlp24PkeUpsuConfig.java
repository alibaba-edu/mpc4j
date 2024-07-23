package edu.alibaba.mpc4j.s2pc.upso.upsu.zlp24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc.EccDokvsFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.vectorized.VectorizedStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.upso.upsu.UpsuConfig;

import static edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc.EccDokvsFactory.EccDokvsType;
import static edu.alibaba.mpc4j.common.structure.okve.dokvs.zp.ZpDokvsFactory.ZpDokvsType;
import static edu.alibaba.mpc4j.s2pc.upso.upsu.UpsuFactory.UpsuType;

/**
 * ZLP24 UPSU config.
 *
 * @author Liqiang Peng
 * @date 2024/3/17
 */
public class Zlp24PkeUpsuConfig extends AbstractMultiPartyPtoConfig implements UpsuConfig {
    /**
     * Zp-DOKVS type
     */
    private final ZpDokvsType zpDokvsType;
    /**
     * ECC-DOKVS type
     */
    private final EccDokvsType eccDokvsType;
    /**
     * compress encode
     */
    private final boolean compressEncode;
    /**
     * batch index PIR config
     */
    private final StdIdxPirConfig batchIndexPirConfig;
    /**
     * core COT config
     */
    private final CoreCotConfig coreCotConfig;

    public Zlp24PkeUpsuConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.batchIndexPirConfig, builder.coreCotConfig);
        eccDokvsType = builder.eccDokvsType;
        zpDokvsType = EccDokvsFactory.getCorrespondingEccDokvsType(eccDokvsType);
        compressEncode = builder.compressEncode;
        batchIndexPirConfig = builder.batchIndexPirConfig;
        coreCotConfig = builder.coreCotConfig;
    }

    @Override
    public UpsuType getPtoType() {
        return UpsuType.ZLP24_PKE;
    }

    public EccDokvsType getEccDokvsType() {
        return eccDokvsType;
    }

    public boolean isCompressEncode() {
        return compressEncode;
    }

    public StdIdxPirConfig getBatchIndexPirConfig() {
        return batchIndexPirConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public ZpDokvsType getZpDokvsType() {
        return zpDokvsType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Zlp24PkeUpsuConfig> {
        /**
         * ECC-DOKVS type
         */
        private EccDokvsType eccDokvsType;
        /**
         * compress encode
         */
        private boolean compressEncode;
        /**
         * batch index PIR config
         */
        private StdIdxPirConfig batchIndexPirConfig;
        /**
         * core COT config
         */
        private CoreCotConfig coreCotConfig;

        public Builder() {
            eccDokvsType = EccDokvsType.H3_SPARSE_CLUSTER_BLAZE_GCT;
            compressEncode = true;
            batchIndexPirConfig = new VectorizedStdIdxPirConfig.Builder().build();
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setEccDokvsType(EccDokvsType eccDokvsType) {
            this.eccDokvsType = eccDokvsType;
            return this;
        }

        public Builder setCompressEncode(boolean compressEncode) {
            this.compressEncode = compressEncode;
            return this;
        }

        public Builder setStdIdxPirConfig(StdIdxPirConfig batchIndexPirConfig) {
            this.batchIndexPirConfig = batchIndexPirConfig;
            return this;
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        @Override
        public Zlp24PkeUpsuConfig build() {
            return new Zlp24PkeUpsuConfig(this);
        }
    }
}
