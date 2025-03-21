package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.msp.bcg19;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * BCG19-REG-MSP-VOLE protocol description. In the following paper:
 * <p>
 * Yang, Kang, Chenkai Weng, Xiao Lan, Jiang Zhang, and Xiao Wang. Ferret: Fast extension for correlated OT with small
 * communication. CCS 2020, pp. 1607-1626. 2020.
 * </p>
 * Section 6, we can see that:
 * <p>
 * Protocol Π_{MPCOT} described in Figure 7 assumes that the receiver’s input Q can be any t-sized subset of [n].
 * However, if we assume that LPN with a regular noise distribution, then the set Q is more restricted in which there
 * will be exactly one index in each interval U_i = [i * n / t, (i + 1) * n / t) for i ∈ [t]. In this case, we can
 * construct a more efficient MPCOT protocol by directly using SPCOT. In particular, we can just call F_{SPCOT} t times,
 * each corresponds to an interval U_i of size n/t. The final output is the concatenation of all t output vectors.
 * </p>
 * Following back, we find that the protocol is described in the following paper:
 * <p>
 * Boyle, Elette, Geoffroy Couteau, Niv Gilboa, Yuval Ishai, Lisa Kohl, Peter Rindal, and Peter Scholl. Efficient
 * two-round OT extension and silent non-interactive secure computation. CCS 2019, pp. 291-308. 2019.
 * </p>
 * The following paper introduce this technique into VOLE:
 * <p>
 * Weng, Chenkai, Kang Yang, Jonathan Katz, and Xiao Wang. Wolverine: fast, scalable, and communication-efficient
 * zero-knowledge proofs for boolean and arithmetic circuits. S&P 2021, pp. 1074-1091. IEEE, 2021.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/7/23
 */
class Bcg19RegGf2kMspVolePtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8522955282621429171L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "BCG19_REG_GF2K_MSP_VOLE";
    /**
     * singleton mode
     */
    private static final Bcg19RegGf2kMspVolePtoDesc INSTANCE = new Bcg19RegGf2kMspVolePtoDesc();

    /**
     * private constructor.
     */
    private Bcg19RegGf2kMspVolePtoDesc() {
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
