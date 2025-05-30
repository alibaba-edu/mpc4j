package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.cw;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Constant-weight PIR client Pto Desc
 *
 * @author Qixian Zhou
 * @date 2023/6/18
 */
public class CwStdIdxPirPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4906231290156230509L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CONSTANT_WEIGHT_PIR";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * client sends public keys
         */
        CLIENT_SEND_PUBLIC_KEYS,
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
    private static final CwStdIdxPirPtoDesc INSTANCE = new CwStdIdxPirPtoDesc();

    /**
     * private constructor.
     */
    private CwStdIdxPirPtoDesc() {
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
