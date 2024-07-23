package edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl.rrgg21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl.ZlB2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl.ZlB2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl.ZlExtensionConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl.ZlExtensionFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.wrap.zl.ZlWrapConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.wrap.zl.ZlWrapFactory;

/**
 * RRGG21 Zl Value Extension Config.
 *
 * @author Liqiang Peng
 * @date 2024/5/30
 */
public class Rrgg21ZlExtensionConfig extends AbstractMultiPartyPtoConfig implements ZlExtensionConfig {
    /**
     * b2a config
     */
    private final ZlB2aConfig b2aConfig;
    /**
     * wrap config
     */
    private final ZlWrapConfig wrapConfig;

    private Rrgg21ZlExtensionConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.b2aConfig, builder.wrapConfig);
        b2aConfig = builder.b2aConfig;
        wrapConfig = builder.wrapConfig;
    }

    @Override
    public ZlExtensionFactory.ZlExtensionType getPtoType() {
        return ZlExtensionFactory.ZlExtensionType.RRGG21;
    }

    @Override
    public boolean isSigned() {
        return false;
    }

    public ZlWrapConfig getZlWrapConfig() {
        return wrapConfig;
    }

    public ZlB2aConfig getB2aConfig() {
        return b2aConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rrgg21ZlExtensionConfig> {
        /**
         * b2a config
         */
        private ZlB2aConfig b2aConfig;
        /**
         * wrap config
         */
        private ZlWrapConfig wrapConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            b2aConfig = ZlB2aFactory.createDefaultConfig(securityModel, silent);
            wrapConfig = ZlWrapFactory.createDefaultConfig(securityModel, silent);
        }

        public Builder setZlB2aConfig(ZlB2aConfig b2aConfig) {
            this.b2aConfig = b2aConfig;
            return this;
        }

        public Builder setZlWrapConfig(ZlWrapConfig wrapConfig) {
            this.wrapConfig = wrapConfig;
            return this;
        }

        @Override
        public Rrgg21ZlExtensionConfig build() {
            return new Rrgg21ZlExtensionConfig(this);
        }
    }
}
