package edu.alibaba.mpc4j.s2pc.pir.payable.zlp23;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * ZLP23 payable PIR protocol description.
 *
 * @author Liqiang Peng
 * @date 2023/9/7
 */
public class Zlp23PayablePirPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 9084416397679213841L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "ZLP23_PAYABLE_PIR";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * client send blind
         */
        CLIENT_SEND_BLIND,
        /**
         * server send blind prf
         */
        SERVER_SEND_BLIND_PRF,
    }

    /**
     * the singleton mode
     */
    private static final Zlp23PayablePirPtoDesc INSTANCE = new Zlp23PayablePirPtoDesc();

    /**
     * private constructor.
     */
    private Zlp23PayablePirPtoDesc() {
        // empty
    }

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
