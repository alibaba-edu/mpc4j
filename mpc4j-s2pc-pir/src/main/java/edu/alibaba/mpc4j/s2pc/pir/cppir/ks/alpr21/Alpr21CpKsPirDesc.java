package edu.alibaba.mpc4j.s2pc.pir.cppir.ks.alpr21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * ALPR21 client-specific preprocessing KSPIR protocol description. This protocol introduces a KSPIR construction
 * based on any client-specific preprocessing PIR protocol, it comes from the following paper:
 * <p>
 * A. Ali and T. Lepoint and S. Patel and M. Raykova and P. Schoppmann and K. Seth and K. Yeo
 * Communication-Computation Trade-offs in PIR. In 2021 USENIX Security Symposium. 2021, 1811-1828.
 * </p>
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
class Alpr21CpKsPirDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7098303098425924559L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "ALPR21_CP_KS_PIR";

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
         * server send item response
         */
        SERVER_SEND_RESPONSE,
    }

    /**
     * the singleton mode
     */
    private static final Alpr21CpKsPirDesc INSTANCE = new Alpr21CpKsPirDesc();

    /**
     * private constructor.
     */
    private Alpr21CpKsPirDesc() {
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
