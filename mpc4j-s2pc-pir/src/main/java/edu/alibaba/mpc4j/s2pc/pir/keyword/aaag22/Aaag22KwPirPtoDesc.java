package edu.alibaba.mpc4j.s2pc.pir.keyword.aaag22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * AAAG22 keyword PIR protocol description. The protocol comes from the following paper:
 * <p>
 * Ishtiyaque Ahmad, Divyakant Agrawal, Amr El Abbadi, and Trinabh Gupta.
 * Pantheon: Private Retrieval from Public Key-Value Store. VLDB 2022, pp. 643-656. 2022.
 * </p>
 *
 * @author Liqiang Peng
 * @date 2023/6/16
 */
public class Aaag22KwPirPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4851719210623378754L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "AAAG22_KEYWORD_PIR";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * server send cuckoo hash keys
         */
        SERVER_SEND_PRF_KEY,
        /**
         * client send encryption params
         */
        CLIENT_SEND_PUBLIC_KEYS,
        /**
         * client send query
         */
        CLIENT_SEND_QUERY,
        /**
         * server send item response
         */
        SERVER_SEND_RESPONSE,
    }

    /**
     * the singleton mode
     */
    private static final Aaag22KwPirPtoDesc INSTANCE = new Aaag22KwPirPtoDesc();

    /**
     * private constructor.
     */
    private Aaag22KwPirPtoDesc() {
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
