package edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * SPAM client-specific preprocessing PIR protocol description. The protocol is described in the following paper:
 * <p>
 * Mughees, Muhammad Haris, Sun I and Ling Ren. Simple and Practical Amortized Single-Server Sublinear Private Information
 * Retrieval. Cryptology ePrint Archive (2023).
 * </p>
 * The scheme does not have a name, and we named it to SPAM (Simple and Practical AMortized).
 *
 * @author Weiran Liu
 * @date 2023/8/30
 */
class SpamSingleCpPirDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8544855608133186603L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "SPAM_CPPIR";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * server sends the stream database request
         */
        SERVER_SEND_STREAM_DATABASE_REQUEST,
        /**
         * client sends the stream database response
         */
        CLIENT_SEND_STREAM_DATABASE_RESPONSE,
        /**
         * client send query
         */
        CLIENT_SEND_QUERY,
        /**
         * server send response
         */
        SERVER_SEND_RESPONSE,
    }

    /**
     * the singleton mode
     */
    private static final SpamSingleCpPirDesc INSTANCE = new SpamSingleCpPirDesc();

    /**
     * private constructor.
     */
    private SpamSingleCpPirDesc() {
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
