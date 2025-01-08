package edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * Simple client-specific preprocessing index PIR protocol description. The protocol comes from the following paper:
 * <p>
 * Alexandra Henzinger, Matthew M. Hong, Henry Corrigan-Gibbs, Sarah Meiklejohn, and Vinod Vaikuntanathan.
 * One Server for the Price of Two: Simple and Fast Single-Server Private Information Retrieval. USENIX Security 2023.
 * </p>
 * The implementation is based on
 * <a href="https://github.com/ahenzinger/simplepir">https://github.com/ahenzinger/simplepir</a>.
 *
 * @author Liqiang Peng
 * @date 2023/9/18
 */
public class SimpleCpIdxPirPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 1276797068183810774L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "SIMPLE_CP_IDX_PIR";

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
    private static final SimpleCpIdxPirPtoDesc INSTANCE = new SimpleCpIdxPirPtoDesc();

    /**
     * private constructor.
     */
    private SimpleCpIdxPirPtoDesc() {
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
     * Computes max byteL for each partitioned database.
     *
     * @param n number of entries.
     * @return max byteL for each partitioned database.
     */
    public static int getMaxSubByteL(int n) {
        MathPreconditions.checkPositive("n", n);
        // each row (√n) should place at least one entry, therefore l <= √n.
        return (int) Math.ceil(Math.sqrt(n));
    }

    /**
     * Computes the matrix size. The result is in the form [rows, columns, partitions].
     *
     * @param n     number of entries.
     * @param byteL byteL.
     * @return [rows, columns, partitions].
     */
    public static int[] getMatrixSize(int n, int byteL) {
        MathPreconditions.checkPositive("n", n);
        MathPreconditions.checkPositive("byteL", byteL);
        int subByteL = Math.min(byteL, getMaxSubByteL(n));
        int partition = CommonUtils.getUnitNum(byteL, subByteL);
        // we treat plaintext modulus as p = 2^8, so that the database can be seen as N rows and byteL columns.
        long entryNum = (long) subByteL * n;
        int sqrt = (int) Math.ceil(Math.sqrt(entryNum));
        // ensure that each row can contain at least one element
        sqrt = Math.max(sqrt, subByteL);
        // we make DB rows a little bit larger than rows to ensure each column can contain more data.
        int rowEntryNum = CommonUtils.getUnitNum(sqrt, subByteL);
        int rows = rowEntryNum * subByteL;
        int columns = (int) Math.ceil((double) entryNum / rows);
        assert (long) rows * columns >= entryNum;

        return new int[]{rows, columns, partition};
    }
}
