package edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CMG21 UPSI protocol description. The protocol comes from the following paper:
 * <p>
 * Kelong Cong, Radames Cruz Moreno, Mariana Botelho da Gama, Wei Dai, Ilia Iliashenko, Kim Laine, and Michael
 * Rosenberg. Labeled psi from homomorphic encryption with reduced computation and communication. ACM CCS 2021, pp.
 * 1135-1150. 2021.
 * </p>
 *
 * @author Liqiang Peng
 * @date 2022/5/26
 */
public class Cmg21UpsiPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6265841553375230711L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CMG21_UPSI";

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
    private static final Cmg21UpsiPtoDesc INSTANCE = new Cmg21UpsiPtoDesc();

    /**
     * private constructor.
     */
    private Cmg21UpsiPtoDesc() {
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
