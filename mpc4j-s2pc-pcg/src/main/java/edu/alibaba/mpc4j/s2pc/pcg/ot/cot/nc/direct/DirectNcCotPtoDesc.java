package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.direct;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * 直接NC-COT协议信息。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
class DirectNcCotPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 6192153866874601909L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "DIRECT_NC_COT";

    /**
     * 单例模式
     */
    private static final DirectNcCotPtoDesc INSTANCE = new DirectNcCotPtoDesc();

    /**
     * 私有构造函数
     */
    private DirectNcCotPtoDesc() {
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
