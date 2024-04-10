package edu.alibaba.mpc4j.s2pc.upso.okvr.kw;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirConfig;
import edu.alibaba.mpc4j.s2pc.upso.okvr.OkvrConfig;
import edu.alibaba.mpc4j.s2pc.upso.okvr.OkvrFactory.OkvrType;

/**
 * Keyword PIR OKVR config.
 *
 * @author Weiran Liu
 * @date 2024/2/3
 */
public class KwOkvrConfig extends AbstractMultiPartyPtoConfig implements OkvrConfig {
    /**
     * single-query OPRF config
     */
    private final SqOprfConfig sqOprfConfig;
    /**
     * OKVS type
     */
    private final Gf2eDokvsType okvsType;
    /**
     * Keyword PIR config
     */
    private final KwPirConfig kwPirConfig;

    private KwOkvrConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.sqOprfConfig, builder.kwPirConfig);
        sqOprfConfig = builder.sqOprfConfig;
        okvsType = builder.okvsType;
        kwPirConfig = builder.kwPirConfig;
    }

    @Override
    public OkvrType getPtoType() {
        return OkvrType.KW;
    }

    public SqOprfConfig getSqOprfConfig() {
        return sqOprfConfig;
    }

    public Gf2eDokvsType getOkvsType() {
        return okvsType;
    }

    public KwPirConfig getKwPirConfig() {
        return kwPirConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<KwOkvrConfig> {
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
        private KwPirConfig kwPirConfig;

        public Builder() {
            sqOprfConfig = SqOprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            okvsType = Gf2eDokvsType.H2_SPARSE_CLUSTER_BLAZE_GCT;
            assert Gf2eDokvsFactory.isSparse(okvsType);
            kwPirConfig = new Cmg21KwPirConfig.Builder().build();
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

        public Builder setKwPirConfig(KwPirConfig kwPirConfig) {
            this.kwPirConfig = kwPirConfig;
            return this;
        }

        @Override
        public KwOkvrConfig build() {
            return new KwOkvrConfig(this);
        }
    }
}
