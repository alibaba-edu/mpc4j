package edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.rs21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * RS21 client-payload circuit PSI protocol description. The protocol is described in the following paper:
 * <p>
 * Rindal, Peter, and Phillipp Schoppmann. VOLE-PSI: fast OPRF and circuit-PSI from vector-OLE. EUROCRYPT 2021,
 * pp. 901-930. Cham: Springer International Publishing, 2021.
 * </p>
 * The implementation has linear communication with stash-less cuckoo hashing.
 *
 * @author Weiran Liu
 * @date 2023/7/28
 */
class Rs21CcpsiPtoDesc implements PtoDesc {
    /**
     * the protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 5251732274948480150L);
    /**
     * the protocol name
     */
    private static final String PTO_NAME = "RS21-CCPSI";
    /**
     * the singleton mode
     */
    private static final Rs21CcpsiPtoDesc INSTANCE = new Rs21CcpsiPtoDesc();

    /**
     * private constructor.
     */
    private Rs21CcpsiPtoDesc() {
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
