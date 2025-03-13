package edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.naive;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * naive order-by protocol description
 * the payload is involved during sorting, and the sorting algorithm is the bitonic sort
 *
 * @author Feng Han
 * @date 2025/3/4
 */
public class NaiveOrderByPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -8460446233417169662L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "ORDER_BY_NAIVE";

    /**
     * singleton mode
     */
    private static final NaiveOrderByPtoDesc INSTANCE = new NaiveOrderByPtoDesc();

    /**
     * private constructor
     */
    private NaiveOrderByPtoDesc() {
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
