package edu.alibaba.mpc4j.s2pc.pir.index.single.doublepir;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;

/**
 * Double PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/6/2
 */
public class Hhcm23DoubleSingleIndexPirConfig extends AbstractMultiPartyPtoConfig implements SingleIndexPirConfig {

    public Hhcm23DoubleSingleIndexPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public SingleIndexPirFactory.SingleIndexPirType getProType() {
        return SingleIndexPirFactory.SingleIndexPirType.DOUBLE_PIR;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Hhcm23DoubleSingleIndexPirConfig> {

        public Builder() {
            // empty
        }

        @Override
        public Hhcm23DoubleSingleIndexPirConfig build() {
            return new Hhcm23DoubleSingleIndexPirConfig(this);
        }
    }
}
