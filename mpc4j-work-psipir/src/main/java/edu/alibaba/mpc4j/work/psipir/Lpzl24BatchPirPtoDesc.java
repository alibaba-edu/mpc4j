package edu.alibaba.mpc4j.work.psipir;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * PSI-PIR protocol.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class Lpzl24BatchPirPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 3563364173424535189L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PSI_PIR";

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
         * serve send response
         */
        SERVER_SEND_RESPONSE,
        /**
         * client send blind elements
         */
        CLIENT_SEND_BLIND,
        /**
         * serve send blind element prf
         */
        SERVER_SEND_BLIND_PRF,
        /**
         * client send public keys
         */
        CLIENT_SEND_PUBLIC_KEYS,
    }

    /**
     * the singleton mode
     */
    private static final Lpzl24BatchPirPtoDesc INSTANCE = new Lpzl24BatchPirPtoDesc();

    /**
     * private constructor.
     */
    private Lpzl24BatchPirPtoDesc() {
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
