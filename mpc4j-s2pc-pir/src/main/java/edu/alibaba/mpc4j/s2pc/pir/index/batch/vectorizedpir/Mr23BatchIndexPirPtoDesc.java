package edu.alibaba.mpc4j.s2pc.pir.index.batch.vectorizedpir;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Vectorized batch PIR protocol description. The protocol comes from the following paper:
 * <p>
 * Muhammad Haris Mughees and Ling Ren. Vectorized Batch Private Information Retrieval.
 * To appear in 44th IEEE Symposium on Security and Privacy, 2023.
 * </p>
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class Mr23BatchIndexPirPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6854774536447892257L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "MR23_BATCH_PIR";

    /**
     * protocol step
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
    private static final Mr23BatchIndexPirPtoDesc INSTANCE = new Mr23BatchIndexPirPtoDesc();

    /**
     * private constructor.
     */
    private Mr23BatchIndexPirPtoDesc() {
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
