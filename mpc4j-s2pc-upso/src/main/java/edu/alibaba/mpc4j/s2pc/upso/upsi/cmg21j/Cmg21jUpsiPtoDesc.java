package edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21j;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CMG21J UPSI protocol description. The protocol comes from the following paper:
 * <p>
 * Kelong Cong, Radames Cruz Moreno, Mariana Botelho da Gama, Wei Dai, Ilia Iliashenko, Kim Laine, and Michael
 * Rosenberg. Labeled psi from homomorphic encryption with reduced computation and communication. ACM CCS 2021, pp.
 * 1135-1150. 2021.
 * </p>
 *
 * @author Liqiang Peng
 * @date 2024/2/23
 */
public class Cmg21jUpsiPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8851089296840342791L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CMG21_JAVA_UPSI";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * client sends cuckoo hash keys
         */
        CLIENT_SEND_CUCKOO_HASH_KEYS,
        /**
         * client sends encryption params
         */
        CLIENT_SEND_ENCRYPTION_PARAMS,
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
    private static final Cmg21jUpsiPtoDesc INSTANCE = new Cmg21jUpsiPtoDesc();

    /**
     * private constructor.
     */
    private Cmg21jUpsiPtoDesc() {
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
