package edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirFactory.CpIdxPirType;

/**
 * Double client-specific preprocessing index PIR config.
 *
 * @author Weiran Liu
 * @date 2024/7/8
 */
public class DoubleCpIdxPirConfig extends AbstractMultiPartyPtoConfig implements CpIdxPirConfig {

    public DoubleCpIdxPirConfig() {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public CpIdxPirType getPtoType() {
        return CpIdxPirType.DOUBLE;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<DoubleCpIdxPirConfig> {

        public Builder() {
            // empty
        }

        @Override
        public DoubleCpIdxPirConfig build() {
            return new DoubleCpIdxPirConfig();
        }
    }
}
