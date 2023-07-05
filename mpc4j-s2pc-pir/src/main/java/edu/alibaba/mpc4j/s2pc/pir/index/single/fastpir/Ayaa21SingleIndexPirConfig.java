package edu.alibaba.mpc4j.s2pc.pir.index.single.fastpir;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;

/**
 * FastPIR config.
 *
 * @author Liqiang Peng
 * @date 2023/1/18
 */
public class Ayaa21SingleIndexPirConfig extends AbstractMultiPartyPtoConfig implements SingleIndexPirConfig {

    public Ayaa21SingleIndexPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public SingleIndexPirFactory.SingleIndexPirType getProType() {
        return SingleIndexPirFactory.SingleIndexPirType.FAST_PIR;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ayaa21SingleIndexPirConfig> {

        public Builder() {
            // empty
        }

        @Override
        public Ayaa21SingleIndexPirConfig build() {
            return new Ayaa21SingleIndexPirConfig(this);
        }
    }
}
