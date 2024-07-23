package edu.alibaba.mpc4j.work.payable.pir.zlp24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * ZLP24 payable PIR protocol description.
 *
 * @author Liqiang Peng
 * @date 2024/7/2
 */
public class Zlp24PayablePirPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 9084416397679213841L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "ZLP24_PAYABLE_PIR";

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
    private static final Zlp24PayablePirPtoDesc INSTANCE = new Zlp24PayablePirPtoDesc();

    /**
     * private constructor.
     */
    private Zlp24PayablePirPtoDesc() {
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
