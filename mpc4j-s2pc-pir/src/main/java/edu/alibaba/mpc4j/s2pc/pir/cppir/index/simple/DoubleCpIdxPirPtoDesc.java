package edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * Double client-specific preprocessing index PIR protocol description. The protocol comes from the following paper:
 * <p>
 * Alexandra Henzinger, Matthew M. Hong, Henry Corrigan-Gibbs, Sarah Meiklejohn, and Vinod Vaikuntanathan.
 * One Server for the Price of Two: Simple and Fast Single-Server Private Information Retrieval. USENIX Security 2023.
 * </p>
 * The implementation is based on
 * <a href="https://github.com/ahenzinger/simplepir">https://github.com/ahenzinger/simplepir</a>.
 *
 * @author Weiran Liu
 * @date 2024/7/8
 */
class DoubleCpIdxPirPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7642648930781691344L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "DOUBLE_CP_IDX_PIR";

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
    private static final DoubleCpIdxPirPtoDesc INSTANCE = new DoubleCpIdxPirPtoDesc();

    /**
     * private constructor.
     */
    private DoubleCpIdxPirPtoDesc() {
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
     * Computes the matrix size.
     *
     * @param n     number of entries.
     * @return (rows, columns).
     */
    static int[] getMatrixSize(int n) {
        MathPreconditions.checkPositive("n", n);
        // we treat plaintext modulus as p = 2^8, so that the database can be seen as N rows and 1 columns.
        int rows = (int) Math.ceil(Math.sqrt(n));
        // ensure that each row can contain at least one element
        rows = Math.max(rows, 1);
        int columns = (int) Math.ceil((double) n / rows);
        assert (long) rows * columns >= n;

        return new int[]{rows, columns};
    }
}
