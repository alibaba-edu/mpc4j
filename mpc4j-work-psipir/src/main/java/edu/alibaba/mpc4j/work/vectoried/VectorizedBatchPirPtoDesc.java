package edu.alibaba.mpc4j.work.vectoried;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * VECTORIZED_BATCH_PIR protocol description. The protocol comes from the following paper:
 * <p>
 * Muhammad Haris Mughees and Ling Ren. Vectorized Batch Private Information Retrieval.
 * To appear in 44th IEEE Symposium on Security and Privacy, 2023.
 * </p>
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class VectorizedBatchPirPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 5123567432147854656L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "VECTORIZED_BATCH_PIR";

    /**
     * the singleton mode
     */
    private static final VectorizedBatchPirPtoDesc INSTANCE = new VectorizedBatchPirPtoDesc();

    /**
     * private constructor.
     */
    private VectorizedBatchPirPtoDesc() {
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
