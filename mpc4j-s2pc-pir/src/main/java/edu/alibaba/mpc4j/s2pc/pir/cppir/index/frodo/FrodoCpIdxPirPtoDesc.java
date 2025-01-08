package edu.alibaba.mpc4j.s2pc.pir.cppir.index.frodo;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Frodo client-specific preprocessing index PIR protocol description. The protocol comes from the following paper:
 * <p>
 * Alex Davidson, Goncalo Pestana, Sofia Celi. FrodoPIR: Simple, Scalable, Single-Server Private Information Retrieval.
 * PETS 2023.
 * </p>
 * The implementation is based on
 * <a href="https://github.com/ahenzinger/simplepir">https://github.com/ahenzinger/simplepir</a>.
 *
 * @author Weiran Liu
 * @date 2024/7/24
 */
class FrodoCpIdxPirPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 2696471961271200956L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "FRODO_CP_IDX_PIR";

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
    private static final FrodoCpIdxPirPtoDesc INSTANCE = new FrodoCpIdxPirPtoDesc();

    /**
     * private constructor.
     */
    private FrodoCpIdxPirPtoDesc() {
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

    /**
     * LWE dimension, Section 5.2 of the paper requires n = 1774
     */
    static final int N = 1774;
}
