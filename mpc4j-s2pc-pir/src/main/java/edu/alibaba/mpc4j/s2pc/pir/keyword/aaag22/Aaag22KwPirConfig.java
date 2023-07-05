package edu.alibaba.mpc4j.s2pc.pir.keyword.aaag22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirFactory;

/**
 * AAAG22 keyword PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/6/16
 */
public class Aaag22KwPirConfig extends AbstractMultiPartyPtoConfig implements KwPirConfig {

    public Aaag22KwPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public KwPirFactory.KwPirType getProType() {
        return KwPirFactory.KwPirType.AAAG22;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Aaag22KwPirConfig> {

        public Builder() {
            // empty
        }

        @Override
        public Aaag22KwPirConfig build() {
            return new Aaag22KwPirConfig(this);
        }
    }
}
