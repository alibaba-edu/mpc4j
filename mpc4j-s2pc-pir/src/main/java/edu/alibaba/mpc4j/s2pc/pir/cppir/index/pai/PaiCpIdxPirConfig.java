package edu.alibaba.mpc4j.s2pc.pir.cppir.index.pai;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirFactory.CpIdxPirType;

/**
 * Pai client-specific preprocessing index PIR config.
 *
 * @author Weiran Liu
 * @date 2023/9/23
 */
public class PaiCpIdxPirConfig extends AbstractMultiPartyPtoConfig implements CpIdxPirConfig {

    public PaiCpIdxPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public CpIdxPirType getPtoType() {
        return CpIdxPirType.PAI;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<PaiCpIdxPirConfig> {

        public Builder() {
            // empty
        }

        @Override
        public PaiCpIdxPirConfig build() {
            return new PaiCpIdxPirConfig(this);
        }
    }
}
