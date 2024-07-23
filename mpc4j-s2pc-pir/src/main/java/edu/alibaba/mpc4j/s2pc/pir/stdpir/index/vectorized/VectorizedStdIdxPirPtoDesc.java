package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.vectorized;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Vectorized PIR protocol description. The protocol comes from the following paper:
 * <p>
 * Muhammad Haris Mughees and Ling Ren. Vectorized Batch Private Information Retrieval.
 * To appear in 44th IEEE Symposium on Security and Privacy, 2023.
 * </p>
 *
 * @author Liqiang Peng
 * @date 2023/3/6
 */
public class VectorizedStdIdxPirPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6504767861642733857L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "Vectorized_Batch_PIR";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * server send cuckoo hash keys
         */
        SERVER_SEND_CUCKOO_HASH_KEYS,
        /**
         * client send query
         */
        CLIENT_SEND_QUERY,
        /**
         * server send response
         */
        SERVER_SEND_RESPONSE,
        /**
         * client send public keys
         */
        CLIENT_SEND_PUBLIC_KEYS,
    }

    /**
     * the singleton mode
     */
    private static final VectorizedStdIdxPirPtoDesc INSTANCE = new VectorizedStdIdxPirPtoDesc();

    /**
     * private constructor.
     */
    private VectorizedStdIdxPirPtoDesc() {
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
