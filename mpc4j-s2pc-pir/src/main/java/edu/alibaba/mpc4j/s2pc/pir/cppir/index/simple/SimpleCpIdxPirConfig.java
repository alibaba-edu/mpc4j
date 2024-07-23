package edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirFactory.CpIdxPirType;

/**
 * Simple client-specific preprocessing index PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/9/18
 */
public class SimpleCpIdxPirConfig extends AbstractMultiPartyPtoConfig implements CpIdxPirConfig {

    public SimpleCpIdxPirConfig() {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public CpIdxPirType getPtoType() {
        return CpIdxPirType.SIMPLE;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<SimpleCpIdxPirConfig> {

        public Builder() {
            // empty
        }

        @Override
        public SimpleCpIdxPirConfig build() {
            return new SimpleCpIdxPirConfig();
        }
    }
}
