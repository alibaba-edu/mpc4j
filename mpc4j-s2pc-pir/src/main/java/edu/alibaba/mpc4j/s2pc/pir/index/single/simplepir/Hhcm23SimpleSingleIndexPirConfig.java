package edu.alibaba.mpc4j.s2pc.pir.index.single.simplepir;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;

/**
 * Simple PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/5/30
 */
public class Hhcm23SimpleSingleIndexPirConfig extends AbstractMultiPartyPtoConfig implements SingleIndexPirConfig {

    public Hhcm23SimpleSingleIndexPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public SingleIndexPirFactory.SingleIndexPirType getProType() {
        return SingleIndexPirFactory.SingleIndexPirType.SIMPLE_PIR;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Hhcm23SimpleSingleIndexPirConfig> {

        public Builder() {
            // empty
        }

        @Override
        public Hhcm23SimpleSingleIndexPirConfig build() {
            return new Hhcm23SimpleSingleIndexPirConfig(this);
        }
    }
}
