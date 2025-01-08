package edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.cgp20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Partial ST protocol description, nothing changed in CGP20
 *
 * @author Feng Han
 * @date 2024/8/6
 */
public class Cgp20PstPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -1935788423408898229L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CGP20_PST";

    /**
     * private constructor.
     */
    private Cgp20PstPtoDesc() {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Cgp20PstPtoDesc INSTANCE = new Cgp20PstPtoDesc();

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
