package edu.alibaba.mpc4j.work.payable.psi.zlp24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * ZLP24 payable PSI protocol description.
 *
 * @author Liqiang Peng
 * @date 2024/7/1
 */
public class Zlp24PayablePsiPtoDesc implements PtoDesc {

    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 9083316397679213841L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "ZLP24_PAYABLE_PSI";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * server send cuckoo hash keys
         */
        SERVER_SEND_CUCKOO_HASH_KEYS,
        /**
         * client send check
         */
        CLIENT_SEND_CHECK,
        /**
         * server send z
         */
        SERVER_SEND_Z,
    }

    /**
     * the singleton mode
     */
    private static final Zlp24PayablePsiPtoDesc INSTANCE = new Zlp24PayablePsiPtoDesc();

    /**
     * private constructor.
     */
    private Zlp24PayablePsiPtoDesc() {
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