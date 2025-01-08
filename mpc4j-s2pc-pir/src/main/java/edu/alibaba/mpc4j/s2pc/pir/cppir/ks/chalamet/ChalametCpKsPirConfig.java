package edu.alibaba.mpc4j.s2pc.pir.cppir.ks.chalamet;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.CpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.CpKsPirFactory;

/**
 * Chalamet client-specific preprocessing KSPIR protocol description.
 *
 * @author Liqiang Peng
 * @date 2024/8/2
 */
public class ChalametCpKsPirConfig extends AbstractMultiPartyPtoConfig implements CpKsPirConfig {

    public ChalametCpKsPirConfig() {
        super(SecurityModel.SEMI_HONEST);
    }

    @Override
    public CpKsPirFactory.CpKsPirType getPtoType() {
        return CpKsPirFactory.CpKsPirType.CHALAMET;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<ChalametCpKsPirConfig> {

        @Override
        public ChalametCpKsPirConfig build() {
            return new ChalametCpKsPirConfig();
        }
    }
}
