package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.lll24;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.lll24.Lll24SstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.CstRosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnFactory.RosnType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.lll24.Lll24BstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * LLL24 CST Random OSN config.
 *
 * @author Weiran Liu
 * @date 2024/5/9
 */
public class Lll24CstRosnConfig extends AbstractMultiPartyPtoConfig implements CstRosnConfig {
    /**
     * t
     */
    private final int t;
    /**
     * BST
     */
    private final BstConfig bstConfig;
    /**
     * SST
     */
    private final SstConfig sstConfig;
    /**
     * COT config
     */
    private final CotConfig cotConfig;

    private Lll24CstRosnConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.bstConfig, builder.sstConfig, builder.cotConfig);
        this.t = builder.t;
        bstConfig = builder.bstConfig;
        sstConfig = builder.sstConfig;
        cotConfig = builder.cotConfig;
    }

    @Override
    public BstConfig getBstConfig() {
        return bstConfig;
    }

    @Override
    public SstConfig getSstConfig() {
        return sstConfig;
    }

    @Override
    public CotConfig getCotConfig() {
        return cotConfig;
    }

    @Override
    public int getT() {
        return t;
    }

    @Override
    public RosnType getPtoType() {
        return RosnType.LLL24_CST;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Lll24CstRosnConfig> {
        /**
         * t
         */
        private final int t;
        /**
         * BST
         */
        private final BstConfig bstConfig;
        /**
         * SST
         */
        private final SstConfig sstConfig;
        /**
         * COT config
         */
        private final CotConfig cotConfig;

        public Builder(int t, boolean silent) {
            // T > 1
            MathPreconditions.checkGreater("t", t, 1);
            // T = 2^t
            Preconditions.checkArgument(IntMath.isPowerOfTwo(t), "T must be a power of 2: %s", t);
            this.t = t;
            bstConfig = new Lll24BstConfig.Builder().build();
            sstConfig = new Lll24SstConfig.Builder().build();
            cotConfig = CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
        }

        @Override
        public Lll24CstRosnConfig build() {
            return new Lll24CstRosnConfig(this);
        }
    }
}
