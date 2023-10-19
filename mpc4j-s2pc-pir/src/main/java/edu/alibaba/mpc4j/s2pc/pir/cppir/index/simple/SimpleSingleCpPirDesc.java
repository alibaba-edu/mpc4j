package edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Simple client-specific preprocessing PIR protocol description. The protocol comes from the following paper:
 * <p>
 * Alexandra Henzinger, Matthew M. Hong, Henry Corrigan-Gibbs, Sarah Meiklejohn, and Vinod Vaikuntanathan.
 * One Server for the Price of Two: Simple and Fast Single-Server Private Information Retrieval.
 * To appear in 2023 USENIX Security Symposium.
 * </p>
 * The implementation is based on https://github.com/ahenzinger/simplepir.
 *
 * @author Liqiang Peng
 * @date 2023/9/18
 */
class SimpleSingleCpPirDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 1276797068183810774L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "SIMPLE_CPPIR";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * serve send seed
         */
        SERVER_SEND_SEED,
        /**
         * server send hint
         */
        SERVER_SEND_HINT,
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
    private static final SimpleSingleCpPirDesc INSTANCE = new SimpleSingleCpPirDesc();

    /**
     * private constructor.
     */
    private SimpleSingleCpPirDesc() {
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
