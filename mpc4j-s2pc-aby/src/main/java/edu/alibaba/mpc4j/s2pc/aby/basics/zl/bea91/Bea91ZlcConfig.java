package edu.alibaba.mpc4j.s2pc.aby.basics.zl.bea91;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory.ZlcType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.ZlTripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.ZlTripleGenFactory;

/**
 * Bea91 Zl circuit config.
 *
 * @author Weiran Liu
 * @date 2023/5/10
 */
public class Bea91ZlcConfig extends AbstractMultiPartyPtoConfig implements ZlcConfig {
    /**
     * Zl triple generation config
     */
    private final ZlTripleGenConfig zlTripleGenConfig;

    private Bea91ZlcConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.zlTripleGenConfig);
        zlTripleGenConfig = builder.zlTripleGenConfig;
    }

    public ZlTripleGenConfig getZlTripleGenConfig() {
        return zlTripleGenConfig;
    }

    @Override
    public ZlcType getPtoType() {
        return ZlcType.BEA91;
    }

    @Override
    public int defaultRoundNum(int l) {
        return zlTripleGenConfig.defaultRoundNum(l);
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Bea91ZlcConfig> {
        /**
         * Zl triple generation config
         */
        private final ZlTripleGenConfig zlTripleGenConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            zlTripleGenConfig = ZlTripleGenFactory.createDefaultConfig(securityModel, silent);
        }

        @Override
        public Bea91ZlcConfig build() {
            return new Bea91ZlcConfig(this);
        }
    }
}
