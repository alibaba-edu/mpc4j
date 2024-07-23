package edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl.g24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl.ZlB2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl.ZlB2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl.ZlExtensionConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl.ZlExtensionFactory;

/**
 * G24 Zl signed value extension config.
 *
 * @author Li Peng
 * @date 2024/6/20
 */
public class G24ZlExtensionConfig extends AbstractMultiPartyPtoConfig implements ZlExtensionConfig {
    /**
     * b2a config
     */
    private final ZlB2aConfig b2aConfig;

    private G24ZlExtensionConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.b2aConfig);
        b2aConfig = builder.b2aConfig;
    }

    @Override
    public ZlExtensionFactory.ZlExtensionType getPtoType() {
        return ZlExtensionFactory.ZlExtensionType.G24;
    }

    @Override
    public boolean isSigned() {
        return true;
    }

    public ZlB2aConfig getB2aConfig() {
        return b2aConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<G24ZlExtensionConfig> {
        /**
         * b2a config
         */
        private final ZlB2aConfig b2aConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            b2aConfig = ZlB2aFactory.createDefaultConfig(securityModel, silent);
        }

        @Override
        public G24ZlExtensionConfig build() {
            return new G24ZlExtensionConfig(this);
        }
    }
}
