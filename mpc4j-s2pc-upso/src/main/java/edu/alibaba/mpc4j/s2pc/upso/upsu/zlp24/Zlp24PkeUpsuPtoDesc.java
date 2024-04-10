package edu.alibaba.mpc4j.s2pc.upso.upsu.zlp24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * ZLP24 UPSU based on PKE protocol description.
 *
 * @author Liqiang Peng
 * @date 2024/3/17
 */
public class Zlp24PkeUpsuPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 1462870017345130060L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "ZLP24_UPSU_PKE";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * receiver sends public key
         */
        RECEIVER_SEND_PK,
        /**
         * sender sends dokvs keys
         */
        SENDER_SEND_DOKVS_KEYS,
        /**
         * receiver sends okvs dense part
         */
        RECEIVER_SEND_DENSE_OKVS,
        /**
         * receiver sends re-rand kem
         */
        SENDER_SEND_RERAND_KEM,
        /**
         * sender sends re-rand ct
         */
        SENDER_SEND_RERAND_CT,
        /**
         * sender sends encrypted elements
         */
        SENDER_SEND_ENC_ELEMENTS,
    }

    /**
     * the singleton mode
     */
    private static final Zlp24PkeUpsuPtoDesc INSTANCE = new Zlp24PkeUpsuPtoDesc();

    /**
     * private constructor.
     */
    private Zlp24PkeUpsuPtoDesc() {
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
