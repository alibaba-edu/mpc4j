package edu.alibaba.mpc4j.s2pc.pso.psi.pke.hfh99;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * HFH99-byte ecc PSI description. The protocol first comes from the following paper:
 * <p>
 * Huberman B A, Franklin M, Hogg T. Enhancing privacy and trust in electronic communities. FC 1999, Citeseer, pp. 78-86.
 * </p>
 * The concept of this protocol first comes from the following paper:
 * <p>
 * C A Meadows. A more efficient cryptographic match-making protocol for use in the absence of a continuously available
 * third party. S&P 1986, IEEE, 1986, pp. 134–137.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
class Hfh99ByteEccPsiPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 8575563187756603661L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "HFH99_BYTE_ECC_PSI";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 客户端发送H(Y)^β
         */
        CLIENT_SEND_HY_BETA,
        /**
         * 服务端发送H(X)^α
         */
        SERVER_SEND_HX_ALPHA,
        /**
         * 服务端发送H(Y)^βα
         */
        CLIENT_SEND_HY_BETA_ALPHA,
    }

    /**
     * 单例模式
     */
    private static final Hfh99ByteEccPsiPtoDesc INSTANCE = new Hfh99ByteEccPsiPtoDesc();

    /**
     * 私有构造函数
     */
    private Hfh99ByteEccPsiPtoDesc() {
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
