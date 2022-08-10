package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.alsz13;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * ALSZ13-核布尔三元组生成协议信息。下述论文：
 * <p>
 * Asharov G, Lindell Y, Schneider T, et al. More efficient oblivious transfer and extensions for faster secure
 * computation. CCS 2013, ACM, 2013, pp. 535-548.
 * </p>
 * 第5.1节给出了如何应用OT构建布尔三元组：
 * <p>
 * In order to generate a multiplication triple, we first introduce the f^{a}b functionality that is implemented in
 * Algorithm 1 using R-OT. In the f^{ab} functionality, the parties hold no input and receive random bits
 * ((a, u), (b, v)), under the constraint that a ⊙ b = u ⊕ v. Now, note that for a multiplication triple c0 ⊕ c1 =
 * (a0 ⊕ a1) ⊙ (b0 ⊕ b1) = (a0 ⊙ b0) ⊕ (a0 ⊙ b1) ⊕ (a1 ⊙ b0) ⊕ (a1 ⊙ b1). The parties can generate a multiplication
 * triple by invoking the f^{ab} functionality twice: in the first invocation P0 acts as R to obtain (a0, u0) and P1
 * acts as S to obtain (b1, v1) with a0 ⊙ b1 = u0 ⊕ v1; in the second invocation P1 acts as R to obtain (a1, u1) and P0
 * acts as S to obtain (b0, v0) with a1 ⊙ b0 = u1 ⊕ v0. Finally, each Pi sets ci = (ai ⊙ bi) ⊕ ui ⊕ vi. For correctness,
 * observe that c0 ⊕ c1 = ((a0 ⊙ b0) ⊕ u0 ⊕ v0) ⊕ ((a1 ⊙ b1) ⊕ u1 ⊕ v1) = (a0 ⊙ b0) ⊕ (u0 ⊕ v1) ⊕ (u1 ⊕ v0) ⊕ (a1 ⊙ b1)
 * = (a0 ⊙ b0) ⊕ (a0 ⊙ b1) ⊕ (a1 ⊙ b0) ⊕ (a1 ⊙ b1) = (a0 ⊕ a1) ⊙ (b0 ⊕ b1), as required.
 * </p>
 * 当使用安静OT时，我们可以进一步降低f^{ab}的通信量。具体协议描述如下：
 * <p>
 * 1. S and R perform a silent R-OT. S obtains bits x0, x1 and R obtains bit a and x_a as output.<br>
 * 2. R sets u = xa; S sets b = x0 ⊕ x1 and v = x0.<br>
 * 3. R outputs (a, u) and S outputs (b, v).
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/4/6
 */
class Alsz13Z2CoreMtgPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 331941896302807392L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "ALSZ13_Z2_CORE_MTG";

    /**
     * 单例模式
     */
    private static final Alsz13Z2CoreMtgPtoDesc INSTANCE = new Alsz13Z2CoreMtgPtoDesc();

    /**
     * 私有构造函数
     */
    private Alsz13Z2CoreMtgPtoDesc() {
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
