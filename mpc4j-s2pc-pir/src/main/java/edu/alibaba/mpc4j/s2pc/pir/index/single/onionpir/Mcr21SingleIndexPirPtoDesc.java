package edu.alibaba.mpc4j.s2pc.pir.index.single.onionpir;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * OnionPIR protocol description. The protocol comes from the following paper:
 * <p>
 * Muhammad Haris Mughees, Hao Chen, and Ling Ren. OnionPIR: Response Efficient Single-Server PIR.
 * 2021 ACM SIGSAC Conference on Computer and Communications Security. 2021, 15–19
 * </p>
 * The original scheme was implemented using NFLlib and SEAL, here the SEAL implementation is applied.
 *
 * @author Liqiang Peng
 * @date 2022/11/14
 */
public class Mcr21SingleIndexPirPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 1557128141245400138L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "ONION_PIR";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * client send public keys
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
    private static final Mcr21SingleIndexPirPtoDesc INSTANCE = new Mcr21SingleIndexPirPtoDesc();

    /**
     * private constructor.
     */
    private Mcr21SingleIndexPirPtoDesc() {
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
