package edu.alibaba.work.femur.seal;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * PGM-index range SEAL PIR protocol description.
 *
 * @author Liqiang Peng
 * @date 2024/9/10
 */
public class SealFemurRpcPirPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 5671672692952341035L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PGM_RANGE_SEAL_PIR";

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
        /**
         * client sends public keys
         */
        CLIENT_SEND_PUBLIC_KEYS,
    }

    /**
     * the singleton mode
     */
    private static final SealFemurRpcPirPtoDesc INSTANCE = new SealFemurRpcPirPtoDesc();

    /**
     * private constructor.
     */
    private SealFemurRpcPirPtoDesc() {
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
