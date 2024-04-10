package edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.peqt;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * SJ23 unbalanced circuit PSI protocol description. The protocol comes from the construction 1 of the following paper:
 * <p>
 * Pinkas, Benny, Thomas Schneider, Oleksandr Tkachenko, and Avishay Yanai. PSI with computation or Circuit-PSI for
 * Unbalanced Sets from Homomorphic Encryption. AsiaCCS 2023, pp. 342-356.
 * </p>
 *
 * @author Liqiang Peng
 * @date 2023/7/17
 */
class Sj23PeqtUcpsiPtoDesc implements PtoDesc {
    /**
     * the protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 9211090740210414623L);
    /**
     * the protocol name
     */
    private static final String PTO_NAME = "SJ23_PEQT_UCPSI";

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
         * client sends query
         */
        CLIENT_SEND_QUERY,
        /**
         * server sends response
         */
        SERVER_SEND_RESPONSE,
    }

    /**
     * the singleton mode
     */
    private static final Sj23PeqtUcpsiPtoDesc INSTANCE = new Sj23PeqtUcpsiPtoDesc();

    /**
     * private constructor.
     */
    private Sj23PeqtUcpsiPtoDesc() {
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
