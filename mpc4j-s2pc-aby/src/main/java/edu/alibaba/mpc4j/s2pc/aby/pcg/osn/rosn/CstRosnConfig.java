package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn;

import edu.alibaba.mpc4j.common.tool.network.PermutationDecomposer;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstFactory;
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
            PermutationDecomposer decomposer = new PermutationDecomposer(paddingNum, t);
            int g = decomposer.getG();
            int d = decomposer.getD();
            return BstFactory.getPrecomputeNum(getBstConfig(), g * d, t);
        }
    }
}
