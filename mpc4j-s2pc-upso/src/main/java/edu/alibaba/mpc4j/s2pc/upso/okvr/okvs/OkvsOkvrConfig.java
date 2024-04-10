package edu.alibaba.mpc4j.s2pc.upso.okvr.okvs;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.upso.okvr.OkvrConfig;
import edu.alibaba.mpc4j.s2pc.upso.okvr.OkvrFactory.OkvrType;

/**
 * OKVS OKVR config.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
public class OkvsOkvrConfig extends AbstractMultiPartyPtoConfig implements OkvrConfig {
    /**
     * single-query OPRF config
     */
    private final SqOprfConfig sqOprfConfig;
    /**
     * DOKVS type
     */
    private final Gf2eDokvsType okvsType;

    private OkvsOkvrConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.sqOprfConfig);
        sqOprfConfig = builder.sqOprfConfig;
        okvsType = builder.okvsType;
    }

    @Override
    public OkvrType getPtoType() {
        return OkvrType.OKVS;
    }

    public SqOprfConfig getSqOprfConfig() {
        return sqOprfConfig;
    }

    public Gf2eDokvsType getOkvsType() {
        return okvsType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<OkvsOkvrConfig> {
        /**
         * single-point OPRF config
         */
        private SqOprfConfig sqOprfConfig;
        /**
         * DOKVS type
         */
        private Gf2eDokvsType okvsType;

        public Builder() {
            sqOprfConfig = SqOprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            okvsType = Gf2eDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT;
        }

        public Builder setSqOprfConfig(SqOprfConfig sqOprfConfig) {
            this.sqOprfConfig = sqOprfConfig;
            return this;
        }

        public Builder setOkvsType(Gf2eDokvsType okvsType) {
            this.okvsType = okvsType;
            return this;
        }

        @Override
        public OkvsOkvrConfig build() {
            return new OkvsOkvrConfig(this);
        }
    }
}
