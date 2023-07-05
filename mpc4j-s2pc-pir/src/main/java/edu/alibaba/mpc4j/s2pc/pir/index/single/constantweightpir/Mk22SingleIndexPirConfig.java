package edu.alibaba.mpc4j.s2pc.pir.index.single.constantweightpir;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;

/**
 * Constant-weight PIR config
 *
 * @author Qixian Zhou
 * @date 2023/6/18
 */
public class Mk22SingleIndexPirConfig extends AbstractMultiPartyPtoConfig implements SingleIndexPirConfig {

    public Mk22SingleIndexPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public SingleIndexPirFactory.SingleIndexPirType getProType() {
        return SingleIndexPirFactory.SingleIndexPirType.CONSTANT_WEIGHT_PIR;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Mk22SingleIndexPirConfig> {

        public Builder() {
            // empty
        }

        @Override
        public Mk22SingleIndexPirConfig build() {
            return new Mk22SingleIndexPirConfig(this);
        }
    }
}

