package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.cgp20;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.network.decomposer.PermutationDecomposerFactory.DecomposerType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.PstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.cgp20.Cgp20PstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.cgp20.Cgp20SstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.CstRosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnFactory.RosnType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.cgp20.Cgp20BstConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * CGP20 CST Random OSN config.
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
public class Cgp20CstRosnConfig extends AbstractMultiPartyPtoConfig implements CstRosnConfig {
    /**
     * t
     */
    private final int t;
    /**
     * max nt in one batch
     */
    private final int maxNt4Batch;
    /**
     * max storage in one batch
     */
    private final long maxCache4Batch;
    /**
     * permutation decomposer type
     */
    private final DecomposerType decomposerType;
    /**
     * PST
     */
    private final PstConfig pstConfig;
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

    private Cgp20CstRosnConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.bstConfig, builder.bstConfig, builder.sstConfig, builder.cotConfig);
        t = builder.t;
        maxNt4Batch = builder.maxNt4Batch;
        maxCache4Batch = builder.maxCache4Batch;
        decomposerType = builder.decomposerType;
        pstConfig = builder.pstConfig;
        bstConfig = builder.bstConfig;
        sstConfig = builder.sstConfig;
        cotConfig = builder.cotConfig;
    }

    @Override
    public DecomposerType getDecomposerType() {
        return decomposerType;
    }

    @Override
    public PstConfig getPstConfig() {
        return pstConfig;
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
    public int getMaxNt4Batch() {
        return maxNt4Batch;
    }

    @Override
    public long getMaxCache4Batch() {
        return maxCache4Batch;
    }

    @Override
    public RosnType getPtoType() {
        return RosnType.CGP20_CST;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cgp20CstRosnConfig> {
        /**
         * t
         */
        private final int t;
        /**
         * t
         */
        private int maxNt4Batch;
        /**
         * max storage in one batch
         */
        private long maxCache4Batch;
        /**
         * permutation decomposer type
         */
        private DecomposerType decomposerType;
        /**
         * PST
         */
        private final PstConfig pstConfig;
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
            maxNt4Batch = CstRosnConfig.DEFAULT_MAX_NT_FRO_BATCH;
            maxCache4Batch = CstRosnConfig.DEFAULT_MAX_CACHE_FRO_BATCH;
            decomposerType = DecomposerType.CGP20;
            bstConfig = new Cgp20BstConfig.Builder().build();
            pstConfig = new Cgp20PstConfig.Builder(silent).setBstConfig((Cgp20BstConfig) bstConfig).build();
            sstConfig = new Cgp20SstConfig.Builder().build();
            cotConfig = CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
        }

        public Builder setMaxNt4Batch(int maxNt4Batch) {
            this.maxNt4Batch = maxNt4Batch;
            return this;
        }

        public Builder setMaxCache4Batch(long maxCache4Batch) {
            this.maxCache4Batch = maxCache4Batch;
            return this;
        }

        public Builder setDecomposerType(DecomposerType decomposerType) {
            this.decomposerType = decomposerType;
            return this;
        }

        @Override
        public Cgp20CstRosnConfig build() {
            return new Cgp20CstRosnConfig(this);
        }
    }
}
