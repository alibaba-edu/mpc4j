package edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * PIANO client-specific preprocessing PIR protocol description. The protocol is described in the following paper:
 * <p>
 * Zhou, Mingxun, Andrew Park, Elaine Shi, and Wenting Zheng. Piano: Extremely Simple, Single-Server PIR with Sublinear
 * Server Computation. To appear in S&P 2024.
 * </p>
 * We follow the original source code to implement the scheme (with corrections and optimizations).
 * <p>
 * https://github.com/pianopir/Piano-PIR
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
class PianoSingleCpPirDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 5812622422813945499L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PIANO_CPPIR";

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
    private static final PianoSingleCpPirDesc INSTANCE = new PianoSingleCpPirDesc();

    /**
     * private constructor.
     */
    private PianoSingleCpPirDesc() {
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
