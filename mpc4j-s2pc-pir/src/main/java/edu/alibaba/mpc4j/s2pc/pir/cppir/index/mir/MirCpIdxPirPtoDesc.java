package edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * MIR client-specific preprocessing index PIR protocol description. The protocol is described in the following paper:
 * <p>
 * Muhammad Haris Mughees, Sun I and Ling Ren. Simple and Practical Amortized Single-Server Sublinear Private Information
 * Retrieval. Cryptology ePrint Archive (2023).
 * </p>
 * The authors did not give a name for this scheme. In the previous version, we named it to SPAM (Simple and Practical
 * AMortized). Later, we find that the "Plinko" paper (shown below) named the scheme MIR (Mughees, I, Ren). We think
 * this name is better, so we change our implementation following that name.
 * <p>
 * Alexander Hoover, Sarvar Patel, Giuseppe Persiano, Kevin Yeo. Plinko: Single-Server PIR with Efficient Updates via
 * Invertible PRFs. Cryptology {ePrint} Archive, Paper 2024/318.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/8/30
 */
class MirCpIdxPirPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8544855608133186603L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "MIR_CP_IDX_PIR";

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
    private static final MirCpIdxPirPtoDesc INSTANCE = new MirCpIdxPirPtoDesc();

    /**
     * private constructor.
     */
    private MirCpIdxPirPtoDesc() {
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
