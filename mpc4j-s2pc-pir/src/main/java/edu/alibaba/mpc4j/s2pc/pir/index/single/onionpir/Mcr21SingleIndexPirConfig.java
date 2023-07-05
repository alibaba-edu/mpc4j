package edu.alibaba.mpc4j.s2pc.pir.index.single.onionpir;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;

/**
 * OnionPIR config.
 *
 * @author Liqiang Peng
 * @date 2022/11/11
 */
public class Mcr21SingleIndexPirConfig extends AbstractMultiPartyPtoConfig implements SingleIndexPirConfig {

    public Mcr21SingleIndexPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public SingleIndexPirFactory.SingleIndexPirType getProType() {
        return SingleIndexPirFactory.SingleIndexPirType.ONION_PIR;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Mcr21SingleIndexPirConfig> {

        public Builder() {
            // empty
        }

        @Override
        public Mcr21SingleIndexPirConfig build() {
            return new Mcr21SingleIndexPirConfig(this);
        }
    }
}
