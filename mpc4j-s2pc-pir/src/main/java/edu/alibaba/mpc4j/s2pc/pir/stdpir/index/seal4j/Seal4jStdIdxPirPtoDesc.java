package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal4j;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * SEAL PIR protocol description. The protocol comes from the following paper:
 * <p>
 * Sebastian Angel, Hao Chen, Kim Laine, and Srinath Setty. PIR with compressed queries and amortized query processing.
 * In 2018 IEEE Symposium on Security and Privacy. 2018, 962–979
 * </p>
 * The implementation is based on <a href="https://github.com/microsoft/SealPIR">...</a>.
 *
 * @author Liqiang Peng
 * @date 2023/1/17
 */
public class Seal4jStdIdxPirPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4924899152232349360L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "SEAL_PIR";

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
    private static final Seal4jStdIdxPirPtoDesc INSTANCE = new Seal4jStdIdxPirPtoDesc();

    /**
     * private constructor.
     */
    private Seal4jStdIdxPirPtoDesc() {
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
