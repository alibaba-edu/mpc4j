package edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.common.structure.fusefilter.Arity3ByteFuseInstance;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleCpIdxPirPtoDesc;

/**
 * Simple naive client-specific preprocessing KSPIR protocol description.
 *
 * @author Liqiang Peng
 * @date 2024/8/2
 */
class SimpleNaiveCpKsPirDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8850086851816818949L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "SIMPLE_NAIVE_CP_KS_PIR";
    /**
     * digest byte length
     */
    static final int DIGEST_BYTE_L = 8;

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * server sends the fuse filter seed
         */
        SERVER_SEND_FUSE_FILTER_SEED,
    }

    /**
     * the singleton mode
     */
    private static final SimpleNaiveCpKsPirDesc INSTANCE = new SimpleNaiveCpKsPirDesc();

    /**
     * private constructor.
     */
    private SimpleNaiveCpKsPirDesc() {
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
        int concatByteL = byteL + DIGEST_BYTE_L;
        Arity3ByteFuseInstance byteFuseInstance = new Arity3ByteFuseInstance(n, concatByteL);
        int m = byteFuseInstance.filterLength();
        return SimpleCpIdxPirPtoDesc.getMatrixSize(m, concatByteL);
    }
}
