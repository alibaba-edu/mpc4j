package edu.alibaba.mpc4j.s2pc.pir.cppir.index.frodo;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirFactory.CpIdxPirType;

/**
 * Frodo client-preprocessing index PIR config.
 *
 * @author Weiran Liu
 * @date 2024/7/24
 */
public class FrodoCpIdxPirConfig extends AbstractMultiPartyPtoConfig implements CpIdxPirConfig {

    public FrodoCpIdxPirConfig() {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public CpIdxPirType getPtoType() {
        return CpIdxPirType.FRODO;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<FrodoCpIdxPirConfig> {

        public Builder() {
            // empty
        }

        @Override
        public FrodoCpIdxPirConfig build() {
            return new FrodoCpIdxPirConfig();
        }
    }
}
