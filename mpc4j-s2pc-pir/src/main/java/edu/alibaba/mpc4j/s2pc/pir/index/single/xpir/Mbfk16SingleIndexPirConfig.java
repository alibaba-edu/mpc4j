package edu.alibaba.mpc4j.s2pc.pir.index.single.xpir;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;

/**
 * XPIR config.
 *
 * @author Liqiang Peng
 * @date 2022/8/25
 */
public class Mbfk16SingleIndexPirConfig extends AbstractMultiPartyPtoConfig implements SingleIndexPirConfig {

    public Mbfk16SingleIndexPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public SingleIndexPirFactory.SingleIndexPirType getProType() {
        return SingleIndexPirFactory.SingleIndexPirType.XPIR;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Mbfk16SingleIndexPirConfig> {

        public Builder() {
            // empty
        }

        @Override
        public Mbfk16SingleIndexPirConfig build() {
            return new Mbfk16SingleIndexPirConfig(this);
        }
    }
}
