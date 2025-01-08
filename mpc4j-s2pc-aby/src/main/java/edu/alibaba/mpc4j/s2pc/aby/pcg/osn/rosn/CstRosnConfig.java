package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn;

import edu.alibaba.mpc4j.common.tool.network.decomposer.PermutationDecomposer;
import edu.alibaba.mpc4j.common.tool.network.decomposer.PermutationDecomposerFactory;
import edu.alibaba.mpc4j.common.tool.network.decomposer.PermutationDecomposerFactory.DecomposerType;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.PstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.PstFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;

/**
 * CST Rando OSN config.
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
public interface CstRosnConfig extends RosnConfig {
    /**
     * default maximum NT in a single batch when generating BST.
     */
    int DEFAULT_MAX_NT_FRO_BATCH = 1 << 28;
    /**
     * default maximum NT in a single batch when generating BST.
     */
    long DEFAULT_MAX_CACHE_FRO_BATCH = 1L << 32;

    /**
     * Gets permutation decomposer type.
     *
     * @return permutation decomposer type.
     */
    DecomposerType getDecomposerType();

    /**
     * Gets BST config.
     *
     * @return BST config.
     */
    PstConfig getPstConfig();

    /**
     * Gets BST config.
     *
     * @return BST config.
     */
    BstConfig getBstConfig();

    /**
     * Gets SST config.
     *
     * @return SST config.
     */
    SstConfig getSstConfig();

    /**
     * Gets COT config.
     *
     * @return COT config.
     */
    CotConfig getCotConfig();

    /**
     * Gets T.
     *
     * @return T.
     */
    int getT();

    /**
     * Gets maximum NT in a single batch when generating BST.
     *
     * @return MaxNt4Batch.
     */
    int getMaxNt4Batch();

    /**
     * Gets maximum NT in a single batch when generating BST.
     *
     * @return MaxNt4Batch.
     */
    long getMaxCache4Batch();

    /**
     * Gets number of COTs used in OSN.
     *
     * @param num num.
     * @return number of COTs used in OSN.
     */
    default int getCotNum(int num) {
        int t = getT();
        if (num <= t) {
            return SstFactory.getPrecomputeNum(getSstConfig(), num);
        } else {
            int paddingLogNum = LongUtils.ceilLog2(num);
            int paddingNum = (1 << paddingLogNum);
            PermutationDecomposer decomposer = PermutationDecomposerFactory.createComposer(getDecomposerType(), paddingNum, t);
            int d = decomposer.getD();
            if (d == 1) {
                return SstFactory.getPrecomputeNum(getSstConfig(), paddingNum);
            } else {
                int centerLayerOtNum = BstFactory.getPrecomputeNum(getBstConfig(), decomposer.getG(1), decomposer.getT(1));
                int middleLayerOtNum = PstFactory.getPrecomputeNum(getPstConfig(), decomposer.getG(1), decomposer.getT(1));
                int firstLayerOtNum = PstFactory.getPrecomputeNum(getPstConfig(), decomposer.getG(0), decomposer.getT(0));
                if (d > 3) {
                    return centerLayerOtNum + 2 * firstLayerOtNum + (d - 3) * middleLayerOtNum;
                } else {
                    return centerLayerOtNum + 2 * firstLayerOtNum;
                }
            }
        }
    }
}
