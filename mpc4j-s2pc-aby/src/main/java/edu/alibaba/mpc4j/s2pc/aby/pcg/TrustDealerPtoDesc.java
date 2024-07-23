package edu.alibaba.mpc4j.s2pc.aby.pcg;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Trust dealer protocol description.
 *
 * @author Weiran Liu
 * @date 2024/6/28
 */
public class TrustDealerPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 3683922869711433559L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "TRUST_DEALER";

    /**
     * private constructor.
     */
    private TrustDealerPtoDesc() {
        // empty
    }

    /**
     * singleton mode
     */
    private static final TrustDealerPtoDesc INSTANCE = new TrustDealerPtoDesc();

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
