package edu.alibaba.mpc4j.s2pc.pir.cppir.ks.pai;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.CpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.CpKsPirFactory.CpKsPirType;

/**
 * Pai client-specific preprocessing CKSPIR config.
 *
 * @author Liqiang Peng
 * @date 2023/9/20
 */
public class PaiCpCksPirConfig extends AbstractMultiPartyPtoConfig implements CpKsPirConfig {
    /**
     * sq-OPRF config
     */
    private final SqOprfConfig sqOprfConfig;

    public PaiCpCksPirConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.sqOprfConfig);
        sqOprfConfig = builder.sqOprfConfig;
    }

    @Override
    public CpKsPirType getPtoType() {
        return CpKsPirType.PAI_CKS;
    }

    public SqOprfConfig getSqOprfConfig() {
        return sqOprfConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<PaiCpCksPirConfig> {
        /**
         * sq-OPRF config
         */
        private final SqOprfConfig sqOprfConfig;

        public Builder() {
            sqOprfConfig = SqOprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        @Override
        public PaiCpCksPirConfig build() {
            return new PaiCpCksPirConfig(this);
        }
    }
}
