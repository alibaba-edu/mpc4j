package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.fast;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * FastPIR protocol description. The protocol comes from the following paper:
 * <p>
 * Ishtiyaque Ahmad, Yuntian Yang, Divyakant Agrawal, Amr El Abbadi, and Trinabh Gupta.
 * Addra: Metadata-private voice communication over fully untrusted infrastructure.
 * In 15th USENIX Symposium on Operating Systems Design and Implementation, OSDI. 2021, 313-329
 * </p>
 * The implementation is based on <a href="https://github.com/ishtiyaque/FastPIR">...</a>.
 *
 * @author Liqiang Peng
 * @date 2023/1/18
 */
public class FastStdIdxPirPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4569274851469795906L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "FAST_PIR";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * client sends public keys
         */
        CLIENT_SEND_PUBLIC_KEYS,
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
    private static final FastStdIdxPirPtoDesc INSTANCE = new FastStdIdxPirPtoDesc();

    /**
     * private constructor.
     */
    private FastStdIdxPirPtoDesc() {
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
