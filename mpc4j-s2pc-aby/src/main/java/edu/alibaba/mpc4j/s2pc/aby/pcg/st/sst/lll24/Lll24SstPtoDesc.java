package edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.lll24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * LLL24-SST protocol description.
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
class Lll24SstPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4117344506521148630L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "LLL24_SST";

    /**
     * private constructor.
     */
    private Lll24SstPtoDesc() {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Lll24SstPtoDesc INSTANCE = new Lll24SstPtoDesc();

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
