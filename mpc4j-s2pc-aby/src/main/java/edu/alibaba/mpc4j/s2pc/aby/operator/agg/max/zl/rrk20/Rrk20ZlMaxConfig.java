package edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.rrk20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.ZlMaxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.ZlMaxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.max2.zl.ZlMax2Config;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.max2.zl.ZlMax2Factory;

/**
 * RRK+20 Zl Max Config.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class Rrk20ZlMaxConfig extends AbstractMultiPartyPtoConfig implements ZlMaxConfig {
    /**
     * Zl greater config.
     */
    private final ZlMax2Config zlMax2Config;

    private Rrk20ZlMaxConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.zlMax2Config);
        zlMax2Config = builder.zlMax2Config;
    }

    public ZlMax2Config getZlGreaterConfig() {
        return zlMax2Config;
    }

    @Override
    public ZlMaxFactory.ZlMaxType getPtoType() {
        return ZlMaxFactory.ZlMaxType.RRK20;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rrk20ZlMaxConfig> {
        /**
         * Zl greater config.
         */
        private final ZlMax2Config zlMax2Config;

        public Builder(SecurityModel securityModel, boolean silent) {
            zlMax2Config = ZlMax2Factory.createDefaultConfig(securityModel, silent);
        }

        @Override
        public Rrk20ZlMaxConfig build() {
            return new Rrk20ZlMaxConfig(this);
        }
    }
}
