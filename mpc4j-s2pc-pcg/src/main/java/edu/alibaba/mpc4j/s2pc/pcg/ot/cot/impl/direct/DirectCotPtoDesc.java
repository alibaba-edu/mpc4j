package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.direct;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * 直接COT（Direct COT）协议信息，直接调用核COT协议实现COT的功能。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
class DirectCotPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 9072724171080530799L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "DIRECT_COT";

    /**
     * 单例模式
     */
    private static final DirectCotPtoDesc INSTANCE = new DirectCotPtoDesc();

    /**
     * 私有构造函数
     */
    private DirectCotPtoDesc() {
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
