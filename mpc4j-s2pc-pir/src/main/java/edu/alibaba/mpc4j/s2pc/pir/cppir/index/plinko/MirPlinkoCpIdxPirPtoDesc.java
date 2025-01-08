package edu.alibaba.mpc4j.s2pc.pir.cppir.index.plinko;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * MIR-based Plinko client-preprocessing index PIR protocol description.
 *
 * @author Weiran Liu
 * @date 2024/10/11
 */
class MirPlinkoCpIdxPirPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 5580764758445451488L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "MIR_PLINKO_CP_IDX_PIR";
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
    private static final MirPlinkoCpIdxPirPtoDesc INSTANCE = new MirPlinkoCpIdxPirPtoDesc();

    /**
     * private constructor.
     */
    private MirPlinkoCpIdxPirPtoDesc() {
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
