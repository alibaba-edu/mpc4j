package edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * PGM-Index client-specific preprocessing KSPIR protocol description.
 *
 * @author Liqiang Peng
 * @date 2024/8/2
 */
public class SimplePgmCpKsPirDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 2874701500073823140L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "SIMPLE_PGM_CP_KS_PIR";
    /**
     * digest byte length
     */
    static final int DIGEST_BYTE_L = 8;
    /**
     * The epsilon range used to build this index.
     */
    static final int EPSILON = 4;
    /**
     * The recursive epsilon range used to build this index.
     */
    static final int EPSILON_RECURSIVE = 2;

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * server sends the PGM-index info
         */
        SERVER_SEND_PGM_INFO,
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
    private static final SimplePgmCpKsPirDesc INSTANCE = new SimplePgmCpKsPirDesc();

    /**
     * private constructor.
     */
    private SimplePgmCpKsPirDesc() {
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
     * Computes the matrix size. The result is in the form [rows, columns, partitions].
     *
     * @param n     number of entries.
     * @param byteL byteL.
     * @return [rows, columns, partitions].
     */
    static int[] getMatrixSize(int n, int byteL) {
        MathPreconditions.checkPositive("n", n);
        MathPreconditions.checkPositive("byteL", byteL);
        int partition = byteL + DIGEST_BYTE_L;
        int columns = (int) Math.ceil(Math.sqrt((long) n * (long) partition));
        int dataRows = Math.max(CommonUtils.getUnitNum(n, columns), EPSILON + 2);
        return new int[]{dataRows + 2 * EPSILON + 3, columns, partition};
    }
}
