package edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.cgs22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CGS22 unbalanced related-batch OPPRF protocol description. The construction comes from the following paper:
 * <p>
 * Chandran, Nishanth, Divya Gupta, and Akash Shah. Circuit-PSI With Linear Complexity via Relaxed Batch OPPRF.
 * PETS 2022, pp. 353-372.
 * </p>
 * Here we leverage single-query OPRF so that we can generate the hint in the init phase.
 *
 * @author Weiran Liu
 * @date 2023/4/18
 */
class Cgs22UrbopprfPtoDesc implements PtoDesc {
    /**
     * the protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6029438306680255544L);
    /**
     * the protocol name
     */
    private static final String PTO_NAME = "CGS22_URBOPPRF";

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
    private static final Cgs22UrbopprfPtoDesc INSTANCE = new Cgs22UrbopprfPtoDesc();

    /**
     * private constructor.
     */
    private Cgs22UrbopprfPtoDesc() {
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
