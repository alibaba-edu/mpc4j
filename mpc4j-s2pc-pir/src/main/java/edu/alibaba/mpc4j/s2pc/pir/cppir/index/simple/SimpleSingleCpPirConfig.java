package edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleCpPirFactory.SingleCpPirType;

/**
 * Simple client-specific preprocessing PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/9/18
 */
public class SimpleSingleCpPirConfig extends AbstractMultiPartyPtoConfig implements SingleCpPirConfig {

    public SimpleSingleCpPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public SingleCpPirType getPtoType() {
        return SingleCpPirType.SIMPLE;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<SimpleSingleCpPirConfig> {

        public Builder() {
            // empty
        }

        @Override
        public SimpleSingleCpPirConfig build() {
            return new SimpleSingleCpPirConfig(this);
        }
    }
}
