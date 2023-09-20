package edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.pdsm;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * SJ23 unbalanced circuit PSI protocol description. The protocol comes from the construction 2 of the following paper:
 * <p>
 * Pinkas, Benny, Thomas Schneider, Oleksandr Tkachenko, and Avishay Yanai. PSI with computation or Circuit-PSI for
 * Unbalanced Sets from Homomorphic Encryption. AsiaCCS 2023, pp. 342-356.
 * </p>
 *
 * @author Liqiang Peng
 * @date 2023/7/17
 */
class Sj23PdsmUcpsiPtoDesc implements PtoDesc {
    /**
     * the protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 2557540688389675399L);
    /**
     * the protocol name
     */
    private static final String PTO_NAME = "SJ23-UCPSI";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * the sender sends cuckoo hash keys
         */
        SERVER_SEND_HASH_KEYS,
        /**
         * client sends public keys
         */
        CLIENT_SEND_PUBLIC_KEYS,
        /**
         * server sends public keys
         */
        SERVER_SEND_PUBLIC_KEYS,
        /**
         * client sends query
         */
        CLIENT_SEND_QUERY,
        /**
         * server sends response
         */
        SERVER_SEND_RESPONSE,
        /**
         * server sends query
         */
        SERVER_SEND_QUERY,
        /**
         * client sends response
         */
        CLIENT_SEND_RESPONSE,
    }

    /**
     * the singleton mode
     */
    private static final Sj23PdsmUcpsiPtoDesc INSTANCE = new Sj23PdsmUcpsiPtoDesc();

    /**
     * private constructor.
     */
    private Sj23PdsmUcpsiPtoDesc() {
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
