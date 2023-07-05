package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.bea95;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.PreLnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.PreLnotFactory;

/**
 * @author Weiran Liu
 * @date 2023/4/11
 */
public class Bea95PreLnotConfig extends AbstractMultiPartyPtoConfig implements PreLnotConfig {

    private Bea95PreLnotConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public PreLnotFactory.PreLnotType getPtoType() {
        return PreLnotFactory.PreLnotType.Bea95;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Bea95PreLnotConfig> {

        public Builder() {
            // empty
        }

        @Override
        public Bea95PreLnotConfig build() {
            return new Bea95PreLnotConfig(this);
        }
    }
}
