package edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.rs21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * RS21 server-payload circuit PSI protocol description. The protocol is described in the following paper:
 * <p>
 * Rindal, Peter, and Phillipp Schoppmann. VOLE-PSI: fast OPRF and circuit-PSI from vector-OLE. EUROCRYPT 2021,
 * pp. 901-930. Cham: Springer International Publishing, 2021.
 * </p>
 * The implementation has linear communication with stash-less cuckoo hashing.
 *
 * @author Weiran Liu
 * @date 2023/7/27
 */
public class Rs21ScpsiPtoDesc implements PtoDesc {
    /**
     * the protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6977323854840309280L);
    /**
     * the protocol name
     */
    private static final String PTO_NAME = "RS21-SCPSI";
    /**
     * the singleton mode
     */
    private static final Rs21ScpsiPtoDesc INSTANCE = new Rs21ScpsiPtoDesc();

    /**
     * private constructor.
     */
    private Rs21ScpsiPtoDesc() {
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
