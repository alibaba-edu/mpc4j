package edu.alibaba.mpc4j.s2pc.pir.keyword.alpr21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * The protocol introduces a keyword PIR construction based on an index PIR protocol, it comes from the following paper:
 * <p>
 * A. Ali and T. Lepoint and S. Patel and M. Raykova and P. Schoppmann and K. Seth and K. Yeo
 * Communication-Computation Trade-offs in PIR.
 * In 2021 USENIX Security Symposium. 2021, 1811-1828.
 * </p>
 *
 * @author Liqiang Peng
 * @date 2023/7/4
 */
public class Alpr21KwPirPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4681208408191309489L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "ALPR21_KEYWORD_PIR";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * server send cuckoo hash keys
         */
        SERVER_SEND_CUCKOO_HASH_KEYS,
        /**
         * server send prf key
         */
        SERVER_SEND_PRF_KEY,
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
    private static final Alpr21KwPirPtoDesc INSTANCE = new Alpr21KwPirPtoDesc();

    /**
     * private constructor.
     */
    private Alpr21KwPirPtoDesc() {
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
