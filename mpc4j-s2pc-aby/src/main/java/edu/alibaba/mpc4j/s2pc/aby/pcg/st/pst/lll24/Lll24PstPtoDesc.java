package edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.lll24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Partial ST protocol description, where the OT is reduced by extending the number of OT
 *
 * @author Feng Han
 * @date 2024/8/5
 */
public class Lll24PstPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 3961667957218761764L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "LLL24_PST";

    /**
     * private constructor.
     */
    private Lll24PstPtoDesc() {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Lll24PstPtoDesc INSTANCE = new Lll24PstPtoDesc();

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
