package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.gyw23;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * GYW23-SSP-COT protocol description. The construction comes from the following paper:
 * <p>
 * Xiaojie Guo, Kang Yang, Xiao Wang, Wenhao Zhang, Xiang Xie, Jiang Zhang, and Zheli Liu. Half-tree: Halving the cost
 * of tree expansion in COT and DPF. EUROCRYPT 2023, pp. 330-362. Cham: Springer Nature Switzerland, 2023.
 * </p>
 *
 * @author Weiran Liu
 * @date 2024/4/11
 */
class Gyw23SspCotPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 2847355999720370055L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "GYW23_SSP_COT";

    /**
     * private constructor
     */
    private Gyw23SspCotPtoDesc() {
        // empty
    }
    
    /**
     * singleton mode
     */
    private static final Gyw23SspCotPtoDesc INSTANCE = new Gyw23SspCotPtoDesc();

    public static PtoDesc getInstance() {
        return INSTANCE;
    }

    static {
        PtoDescManager.registerPtoDesc(getInstance());
    }

    @Override
    public int getPtoId() {
        return PTO_ID;
    }

    @Override
    public String getPtoName() {
        return PTO_NAME;
    }
}
