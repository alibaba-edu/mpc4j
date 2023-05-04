package edu.alibaba.mpc4j.s2pc.opf.opprf.rb.cgs22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CGS22 Related-Batch OPPRF protocol description. The construction comes from the following paper:
 * <p>
 * Chandran, Nishanth, Divya Gupta, and Akash Shah. Circuit-PSI With Linear Complexity via Relaxed Batch OPPRF.
 * PETS 2022, pp. 353-372.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
class Cgs22RbopprfPtoDesc implements PtoDesc {
    /**
     * the protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 2515868939383425460L);
    /**
     * the protocol name
     */
    private static final String PTO_NAME = "CGS22_RBOPPRF";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * the sender sends garbled table keys
         */
        SENDER_SEND_GARBLED_TABLE_KEYS,
        /**
         * the sender sends the Garbled Hash Table.
         */
        SENDER_SEND_GARBLED_TABLE,
    }

    /**
     * the singleton mode
     */
    private static final Cgs22RbopprfPtoDesc INSTANCE = new Cgs22RbopprfPtoDesc();

    /**
     * private constructor.
     */
    private Cgs22RbopprfPtoDesc() {
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
