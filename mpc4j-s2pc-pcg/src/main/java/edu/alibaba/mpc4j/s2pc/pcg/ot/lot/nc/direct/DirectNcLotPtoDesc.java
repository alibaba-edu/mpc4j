package edu.alibaba.mpc4j.s2pc.pcg.ot.lot.nc.direct;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * 直接NC-2^l选1-OT协议信息。
 *
 * @author Hanwen Feng
 * @date 2022/08/16
 */
public class DirectNcLotPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 7969827722381891450L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "DIRECT_NC_LOT";

    /**
     * 单例模式
     */
    private static final DirectNcLotPtoDesc INSTANCE = new DirectNcLotPtoDesc();

    /**
     * 私有构造函数
     */
    private DirectNcLotPtoDesc() {
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
