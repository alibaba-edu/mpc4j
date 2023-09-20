package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.direct;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * direct no-choice GF2K-VOLE protocol description. This protocol directly invoke core GF2K-VOLE.
 *
 * @author Weiran Liu
 * @date 2023/7/23
 */
public class DirectGf2kNcVolePtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 1454041354130094810L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "DIRECT_GF2K_NC_VOLE";

    /**
     * singleton mode
     */
    private static final DirectGf2kNcVolePtoDesc INSTANCE = new DirectGf2kNcVolePtoDesc();

    /**
     * private constructor
     */
    private DirectGf2kNcVolePtoDesc() {
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
