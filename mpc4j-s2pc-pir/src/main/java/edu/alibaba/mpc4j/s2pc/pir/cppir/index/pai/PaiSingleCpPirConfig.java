package edu.alibaba.mpc4j.s2pc.pir.cppir.index.pai;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleCpPirFactory.SingleCpPirType;

/**
 * Pai client-specific preprocessing PIR config.
 *
 * @author Weiran Liu
 * @date 2023/9/23
 */
public class PaiSingleCpPirConfig extends AbstractMultiPartyPtoConfig implements SingleCpPirConfig {

    public PaiSingleCpPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public SingleCpPirType getPtoType() {
        return SingleCpPirType.PAI;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<PaiSingleCpPirConfig> {

        public Builder() {
            // empty
        }

        @Override
        public PaiSingleCpPirConfig build() {
            return new PaiSingleCpPirConfig(this);
        }
    }
}
