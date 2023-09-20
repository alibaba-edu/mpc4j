package edu.alibaba.mpc4j.s2pc.pir.index.batch.simplepir;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Batch Simple PIR based on Cuckoo Hash protocol description.
 *
 * @author Liqiang Peng
 * @date 2023/7/7
 */
public class CuckooHashBatchSimplePirPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4445246211208037554L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "BATCH_SIMPLE_PIR";

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
         * server send reply
         */
        SERVER_SEND_RESPONSE,
        /**
         * client send public keys
         */
        CLIENT_SEND_PUBLIC_KEYS,
        /**
         * serve send seed
         */
        SERVER_SEND_SEED,
        /**
         * server send hint
         */
        SERVER_SEND_HINT,
    }

    /**
     * the singleton mode
     */
    private static final CuckooHashBatchSimplePirPtoDesc INSTANCE = new CuckooHashBatchSimplePirPtoDesc();

    /**
     * private constructor.
     */
    private CuckooHashBatchSimplePirPtoDesc() {
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
