package edu.alibaba.work.femur.naive;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * PGM-index range naive PIR protocol description.
 *
 * @author Liqiang Peng
 * @date 2024/9/19
 */
public class NaiveFemurRpcPirPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4039428881916366892L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PGM_RANGE_NAIVE_PIR";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * server sends the PGM-index info
         */
        SERVER_SEND_PGM_INFO,
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
    private static final NaiveFemurRpcPirPtoDesc INSTANCE = new NaiveFemurRpcPirPtoDesc();

    /**
     * private constructor.
     */
    private NaiveFemurRpcPirPtoDesc() {
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
