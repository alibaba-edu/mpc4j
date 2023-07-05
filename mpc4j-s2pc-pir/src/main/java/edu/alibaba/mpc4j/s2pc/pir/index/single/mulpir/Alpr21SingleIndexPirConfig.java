package edu.alibaba.mpc4j.s2pc.pir.index.single.mulpir;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;

/**
 * Mul PIR config.
 *
 * @author Qixian Zhou
 * @date 2023/5/29
 */
public class Alpr21SingleIndexPirConfig extends AbstractMultiPartyPtoConfig implements SingleIndexPirConfig {

    public Alpr21SingleIndexPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public SingleIndexPirFactory.SingleIndexPirType getProType() {
        return SingleIndexPirFactory.SingleIndexPirType.MUL_PIR;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Alpr21SingleIndexPirConfig> {

        public Builder() {
            // empty
        }

        @Override
        public Alpr21SingleIndexPirConfig build() {
            return new Alpr21SingleIndexPirConfig(this);
        }
    }
}

