package edu.alibaba.mpc4j.s2pc.pir.cppir.ks.chalamet;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Chalamet client-specific preprocessing KSPIR protocol description.
 *
 * @author Liqiang Peng
 * @date 2024/8/2
 */
public class ChalametCpKsPirDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = (int) Math.abs(4450462727836607535L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CHALAMET_CP_KS_PIR";
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
        /**
         * server sends Frodo PIR seed
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
    private static final ChalametCpKsPirDesc INSTANCE = new ChalametCpKsPirDesc();

    /**
     * private constructor.
     */
    private ChalametCpKsPirDesc() {
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
     * LWE dimension, Section 5.2 of the paper requires n = 1774
     */
    public static final int N = 1774;
}
