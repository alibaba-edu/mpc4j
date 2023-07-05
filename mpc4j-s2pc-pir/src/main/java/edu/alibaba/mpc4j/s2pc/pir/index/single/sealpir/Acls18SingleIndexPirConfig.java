package edu.alibaba.mpc4j.s2pc.pir.index.single.sealpir;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;

/**
 * SEAL PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/1/17
 */
public class Acls18SingleIndexPirConfig extends AbstractMultiPartyPtoConfig implements SingleIndexPirConfig {

    public Acls18SingleIndexPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public SingleIndexPirFactory.SingleIndexPirType getProType() {
        return SingleIndexPirFactory.SingleIndexPirType.SEAL_PIR;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Acls18SingleIndexPirConfig> {

        public Builder() {
            // empty
        }

        @Override
        public Acls18SingleIndexPirConfig build() {
            return new Acls18SingleIndexPirConfig(this);
        }
    }
}
