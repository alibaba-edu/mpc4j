package edu.alibaba.mpc4j.s2pc.pjc.pid.gmr21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * GMR21多点PID协议信息。方案来自于下述论文图第5.5节的Our approach：
 * <p>
 * Garimella G, Mohassel P, Rosulek M, et al. Private Set Operations from Oblivious Switching. PKC 2021, Springer,
 * Cham, pp. 591-617.
 * </p>
 * 原始描述如下：
 * <p>
 * Our approach for private-ID builds on oblivious PRF and private set union. Roughly speaking, suppose the parties run
 * an oblivious PRF twice: first, so that Alice learns k_A and Bob learns PRF(k_A, y_i) for each of his items y_i; and
 * second so that Bob learns k_B and Alice learns PRF(k_B, x_i) for each of her items x_i. We will define the random
 * identifier of an item x as R(x) = RPF(k_A, x) ⊕ PRF(k_B, x).
 * </p>
 * <p>
 * Note that after running the relevant OPRF protocols, both parties can compute R(x) for their own items. To complete
 * the private-ID protocol, they must simply perform a private set union on their sets R(X) and R(Y). This approach
 * indeed leads to a fine private-ID protocol.
 * </p>
 * 作者给出了更高效的PID构造，因此没有直接使用Our approach给出的方法。实际上，our approach方法的计算性能确实更低，但会降低一定的通信量。
 * <p>
 * In the full-version of our paper we present and prove secure an optimization we observe that a full-fledged OPRF is
 * not needed and a so-called “sloppy OPRF” would suffic
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/5/15
 */
class Gmr21MpPidPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 629705786765867756L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "GMR21_MP_PID";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 客户端发送并集
         */
        CLIENT_SEND_UNION,
    }

    /**
     * 单例模式
     */
    private static final Gmr21MpPidPtoDesc INSTANCE = new Gmr21MpPidPtoDesc();

    /**
     * 私有构造函数
     */
    private Gmr21MpPidPtoDesc() {
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
