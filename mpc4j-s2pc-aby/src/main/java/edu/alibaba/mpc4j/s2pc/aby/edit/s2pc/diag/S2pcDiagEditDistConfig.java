package edu.alibaba.mpc4j.s2pc.aby.edit.s2pc.diag;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.edit.DistCmpConfig;
import edu.alibaba.mpc4j.s2pc.aby.edit.EditDistFactory.EditDistType;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl.ZlExtensionConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl.ZlExtensionFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.min2.zl.ZlMin2Config;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.min2.zl.ZlMin2Factory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory;

/**
 * Edit distance config
 *
 * @author Li Peng
 * @date 2024/4/8
 */
public class S2pcDiagEditDistConfig extends AbstractMultiPartyPtoConfig implements DistCmpConfig {
    /**
     * Z2 circuit config.
     */
    private final Z2cConfig z2cConfig;
    /**
     * Zl circuit config.
     */
    private final ZlcConfig zlcConfig;
    /**
     * Private equality test config.
     */
    private final PeqtConfig peqtConfig;
    /**
     * Zl min2 config.
     */
    private final ZlMin2Config zlMin2Config;
    /**
     * Zl mux config.
     */
    private final ZlMuxConfig zlMuxConfig;
    /**
     * Zl extension config.
     */
    private final ZlExtensionConfig zlExtensionConfig;
    /**
     * Max batch size.
     */
    private final int maxBatchSize;
    /**
     * Need to extend zl.
     */
    private final boolean needExtend;
    /**
     * the number of increment of zl length in a single extend step.
     */
    private final int increment;
    /**
     * need to prune unneeded cells.
     */
    private final boolean needPrune;

    private S2pcDiagEditDistConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.z2cConfig,
            builder.peqtConfig, builder.zlMuxConfig, builder.zlExtensionConfig);
        this.maxBatchSize = builder.maxBatchSize;
        this.z2cConfig = builder.z2cConfig;
        this.zlcConfig = builder.zlcConfig;
        this.peqtConfig = builder.peqtConfig;
        this.zlMin2Config = builder.zlMin2Config;
        this.zlMuxConfig = builder.zlMuxConfig;
        this.zlExtensionConfig = builder.zlExtensionConfig;
        this.needExtend = builder.needExtend;
        this.increment = builder.increment;
        this.needPrune = builder.needPrune;
    }

    @Override
    public EditDistType getEditDistType() {
        return EditDistType.S2PC_DIAG_EDIT_DISTANCE;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    public PeqtConfig getPeqtConfig() {
        return peqtConfig;
    }

    public ZlMin2Config getZlMin2Config() {
        return zlMin2Config;
    }

    public ZlMuxConfig getZlMuxConfig() {
        return zlMuxConfig;
    }

    public ZlcConfig getZlcConfig() {
        return zlcConfig;
    }

    public ZlExtensionConfig getZlExtensionConfig() {
        return zlExtensionConfig;
    }

    public boolean isNeedExtend() {
        return needExtend;
    }

    public int getIncrement() {
        return increment;
    }

    public boolean isNeedPrune() {
        return needPrune;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<S2pcDiagEditDistConfig> {
        /**
         * Z2 circuit config.
         */
        private final Z2cConfig z2cConfig;
        /**
         * Zl circuit config.
         */
        private final ZlcConfig zlcConfig;
        /**
         * Private equality test config.
         */
        private final PeqtConfig peqtConfig;
        /**
         * Zl min2 config.
         */
        private final ZlMin2Config zlMin2Config;
        /**
         * Zl mux config.
         */
        private final ZlMuxConfig zlMuxConfig;
        /**
         * Zl extension config.
         */
        private final ZlExtensionConfig zlExtensionConfig;
        /**
         * Max batch size.
         */
        private final int maxBatchSize;
        /**
         * Need to extend zl.
         */
        private boolean needExtend;
        /**
         * the number of increment of zl length in a single extend step.
         */
        private int increment;
        /**
         * need to prune unneeded cells.
         */
        private boolean needPrune;

        public Builder(boolean silent) {
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            zlcConfig = ZlcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            peqtConfig = PeqtFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            zlMin2Config = ZlMin2Factory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            zlMuxConfig = ZlMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            zlExtensionConfig = ZlExtensionFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent, false);
            // hardcode to 100000
            maxBatchSize = 100000;
            needExtend = false;
            increment = 1;
            needPrune = false;
        }

        @Override
        public S2pcDiagEditDistConfig build() {
            return new S2pcDiagEditDistConfig(this);
        }

        public Builder setNeedExtend(boolean needExtend) {
            this.needExtend = needExtend;
            return this;
        }

        public Builder setIncrement(int increment) {
            this.increment = increment;
            return this;
        }

        public Builder setNeedPrune(boolean needPrune) {
            this.needPrune = needPrune;
            return this;
        }
    }
}
