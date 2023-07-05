package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.bea95;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;

/**
 * Bea95 pre-compute COT config.
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public class Bea95PreCotConfig extends AbstractMultiPartyPtoConfig implements PreCotConfig {

    private Bea95PreCotConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public PreCotFactory.PreCotType getPtoType() {
        return PreCotFactory.PreCotType.Bea95;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Bea95PreCotConfig> {

        public Builder() {
            // empty
        }

        @Override
        public Bea95PreCotConfig build() {
            return new Bea95PreCotConfig(this);
        }
    }
}
