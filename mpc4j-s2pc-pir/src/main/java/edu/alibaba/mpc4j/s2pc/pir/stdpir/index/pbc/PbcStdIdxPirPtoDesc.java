package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.pbc;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Probabilistic Batch Code (PBC) Batch Index PIR protocol description.
 * <p></p>
 * The client and the server perform B PIR instances, one for each bucket, to retrieve all the desired
 * entries. This framework is compatible with any single index PIR scheme.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class PbcStdIdxPirPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 2361093113454124451L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PBC_BATCH_PIR";

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
    }

    /**
     * the singleton mode
     */
    private static final PbcStdIdxPirPtoDesc INSTANCE = new PbcStdIdxPirPtoDesc();

    /**
     * private constructor.
     */
    private PbcStdIdxPirPtoDesc() {
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
