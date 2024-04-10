package edu.alibaba.mpc4j.s2pc.upso.upsu.zlp24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * ZLP24 UPSU based on PEQT protocol description.
 *
 * @author Liqiang Peng
 * @date 2024/3/20
 */
public class Zlp24PeqtUpsuPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 744115072810883459L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "ZLP24_UPSU_PEQT";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * receiver sends cuckoo hash keys
         */
        RECEIVER_SEND_CUCKOO_HASH_KEYS,
        /**
         * sender sends dokvs keys
         */
        SENDER_SEND_DOKVS_KEYS,
        /**
         * receiver sends okvs dense part
         */
        RECEIVER_SEND_DENSE_OKVS,
        /**
         * sender sends encrypted elements
         */
        SENDER_SEND_ENC_ELEMENTS,
    }

    /**
     * the singleton mode
     */
    private static final Zlp24PeqtUpsuPtoDesc INSTANCE = new Zlp24PeqtUpsuPtoDesc();

    /**
     * private constructor.
     */
    private Zlp24PeqtUpsuPtoDesc() {
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