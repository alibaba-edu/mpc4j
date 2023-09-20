package edu.alibaba.mpc4j.s2pc.pir.index.batch.naive;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Naive Batch Index PIR protocol description.
 *
 * @author Liqiang Peng
 * @date 2023/7/14
 */
public class NaiveBatchIndexPirPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 1026969684665617736L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "NAIVE_BATCH_PIR";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * server send response
         */
        SERVER_SEND_RESPONSE,
        /**
         * client send query
         */
        CLIENT_SEND_QUERY,
    }

    /**
     * the singleton mode
     */
    private static final NaiveBatchIndexPirPtoDesc INSTANCE = new NaiveBatchIndexPirPtoDesc();

    /**
     * private constructor.
     */
    private NaiveBatchIndexPirPtoDesc() {
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
