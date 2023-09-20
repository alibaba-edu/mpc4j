package edu.alibaba.mpc4j.s2pc.pso.psi.sqoprf.ra17;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * RA17-BYTE_ECC-PSI protocol description. The protocol comes from the following paper:
 * <p>
 * Resende, Amanda C. Davi, and Diego F. Aranha. Faster unbalanced private set intersection. FC 2018, pp. 203-221.
 * Springer Berlin Heidelberg, 2018.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/9/10
 */
class Ra17ByteEccPsiPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 2874621788440711790L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "RA17_BYTE_ECC_PSI";
    /**
     * singleton mode
     */
    private static final Ra17ByteEccPsiPtoDesc INSTANCE = new Ra17ByteEccPsiPtoDesc();

    /**
     * private constructor.
     */
    private Ra17ByteEccPsiPtoDesc() {
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
