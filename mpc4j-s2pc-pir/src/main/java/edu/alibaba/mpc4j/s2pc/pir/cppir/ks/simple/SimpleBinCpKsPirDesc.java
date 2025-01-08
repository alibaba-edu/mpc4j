package edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;

/**
 * Simple bin client-specific preprocessing KSPIR protocol description.
 *
 * @author Liqiang Peng
 * @date 2024/8/2
 */
class SimpleBinCpKsPirDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 137031346370262086L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "SIMPLE_BIN_CP_KS_PIR";
    /**
     * digest byte length
     */
    static final int DIGEST_BYTE_L = 8;

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * server sends the hash bin info
         */
        SERVER_SEND_HASH_BIN_INFO,
        /**
         * server sends Simple PIR seed
         */
        SERVER_SEND_SEED,
        /**
         * server sends hint
         */
        SERVER_SEND_HINT,
        /**
         * client sends query
         */
        CLIENT_SEND_QUERY,
        /**
         * server sends response
         */
        SERVER_SEND_RESPONSE,
    }

    /**
     * the singleton mode
     */
    private static final SimpleBinCpKsPirDesc INSTANCE = new SimpleBinCpKsPirDesc();

    /**
     * private constructor.
     */
    private SimpleBinCpKsPirDesc() {
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
     * Computes max matrix size. The result is in the form [rows, columns, partitions]. Note that SimpleBin scheme
     * requires actual BinSize. Here we just estimate MaxBinSize.
     *
     * @param n     number of entries.
     * @param byteL byteL.
     * @return [rows, columns, partitions].
     */
    static int[] getEstimateMatrixSize(int n, int byteL) {
        MathPreconditions.checkPositive("n", n);
        MathPreconditions.checkPositive("byteL", byteL);
        int partition = byteL + DIGEST_BYTE_L;
        int binNum = (int) Math.ceil(Math.sqrt((long) n * (long) partition));
        int estimateMaxBinSize = MaxBinSizeUtils.approxMaxBinSize(n, binNum);
        return new int[]{estimateMaxBinSize, binNum, partition};
    }
}
