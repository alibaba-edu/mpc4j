package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.pai;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.SingleCpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.SingleCpKsPirFactory.SingleCpKsPirType;

/**
 * Pai client-specific preprocessing CKSPIR config.
 *
 * @author Liqiang Peng
 * @date 2023/9/20
 */
public class PaiSingleCpCksPirConfig extends AbstractMultiPartyPtoConfig implements SingleCpKsPirConfig {
    /**
     * sq-OPRF config
     */
    private final SqOprfConfig sqOprfConfig;

    public PaiSingleCpCksPirConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.sqOprfConfig);
        sqOprfConfig = builder.sqOprfConfig;
    }

    @Override
    public SingleCpKsPirType getPtoType() {
        return SingleCpKsPirType.PAI_CKS;
    }

    public SqOprfConfig getSqOprfConfig() {
        return sqOprfConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<PaiSingleCpCksPirConfig> {
        /**
         * sq-OPRF config
         */
        private SqOprfConfig sqOprfConfig;

        public Builder() {
            sqOprfConfig = SqOprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setSqOprfConfig(SqOprfConfig sqOprfConfig) {
            this.sqOprfConfig = sqOprfConfig;
            return this;
        }

        @Override
        public PaiSingleCpCksPirConfig build() {
            return new PaiSingleCpCksPirConfig(this);
        }
    }
}
