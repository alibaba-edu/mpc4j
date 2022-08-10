package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.iknp03;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * IKNP03-核COT协议信息。论文来源：
 * <p>
 * Ishai Y, Kilian J, Nissim K, et al. Extending oblivious transfers efficiently. CRYPTO 2013, Springer, 2003, pp.
 * 145-161.
 * </p>
 *
 * @author Weiran Liu
 * @date 2020/08/13
 */
class Iknp03CoreCotPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)5305543974056301635L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "IKNP03_CORE_COT";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 接收方发送矩阵
         */
        RECEIVER_SEND_MATRIX,
    }

    /**
     * 单例模式
     */
    private static final Iknp03CoreCotPtoDesc INSTANCE = new Iknp03CoreCotPtoDesc();

    /**
     * 私有构造函数
     */
    private Iknp03CoreCotPtoDesc() {
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
