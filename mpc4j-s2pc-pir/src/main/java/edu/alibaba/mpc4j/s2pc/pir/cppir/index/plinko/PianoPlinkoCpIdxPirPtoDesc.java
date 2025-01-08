package edu.alibaba.mpc4j.s2pc.pir.cppir.index.plinko;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Piano-based Plinko client-specific preprocessing index PIR. The protocol is described in the following paper:
 * <p>
 * Alexander Hoover, Sarvar Patel, Giuseppe Persiano, Kevin Yeo. Plinko: Single-Server PIR with Efficient Updates via
 * Invertible PRFs. Cryptology {ePrint} Archive, Paper 2024/318.
 * </p>
 *
 * @author Weiran Liu
 * @date 2024/10/9
 */
class PianoPlinkoCpIdxPirPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 2008108526500879887L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PIANO_PLINKO_CP_IDX_PIR";

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
        /**
         * server send update
         */
        SERVER_SEND_UPDATE,
    }

    /**
     * the singleton mode
     */
    private static final PianoPlinkoCpIdxPirPtoDesc INSTANCE = new PianoPlinkoCpIdxPirPtoDesc();

    /**
     * private constructor.
     */
    private PianoPlinkoCpIdxPirPtoDesc() {
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
