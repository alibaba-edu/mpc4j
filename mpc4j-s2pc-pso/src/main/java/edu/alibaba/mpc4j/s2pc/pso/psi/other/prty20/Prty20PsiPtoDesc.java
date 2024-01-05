package edu.alibaba.mpc4j.s2pc.pso.psi.other.prty20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.coder.linear.LinearCoderFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;

/**
 * PRTY20 PSI protocol description. The protocol comes from the following paper:
 * <p>
 * Pinkas B, Rosulek M, Trieu N, et al. PSI from PaXoS: Fast, Malicious Private Set Intersection. EUROCRYPT 2020,
 * Springer, Cham, 2020, pp. 739-767.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/9/10
 */
class Prty20PsiPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8534409963311736779L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PRTY20";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * client sends PaXoS key
         */
        CLIENT_SEND_PAXOS_KEY,
        /**
         * server sends PRF filter
         */
        SERVER_SEND_PRF_FILTER,
    }
    /**
     * singleton mode
     */
    private static final Prty20PsiPtoDesc INSTANCE = new Prty20PsiPtoDesc();

    /**
     * private constructor.
     */
    private Prty20PsiPtoDesc() {
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

    /**
     * Gets dataword bit length for the binary coder, which depends on the server element size (l_1) and the client
     * element size (l_2). The paper states that:
     * <p>
     * To instantiate our protocol for semi-honest security, it is enough to set l_1 = l_2 = σ + 2log_2(n), the minimum
     * possible value for security. The issue of extracting a corrupt party’s input, which involves further increasing
     * l_1, l_2, is not relevant in the semi-honest case.
     * </p>
     * <p>
     * It therefore suffices to identify linear (binary) codes with suitable minimum distance, for the different values
     * of l_1 that result. We identify good choices in Figure 4, all of which are the result of concatenating a
     * Reed-Solomon code with a small (optimal) binary code.
     * </p>
     * Therefore, we provide this function for choosing dataword bit length for the binary coder.
     *
     * @param serverElementSize server element size.
     * @param clientElementSize client element size.
     * @return LCOT input bit length.
     */
    @SuppressWarnings("AlibabaUndefineMagicConstant")
    private static int getSemiHonestCoderDatawordBitLength(int serverElementSize, int clientElementSize) {
        MathPreconditions.checkPositiveInRangeClosed("server_element_size", serverElementSize, 1 << 24);
        MathPreconditions.checkPositiveInRangeClosed("client_element_size", clientElementSize, 1 << 24);
        // n = server element size
        if (serverElementSize > (1 << 20)) {
            // 2^20 < n <= 2^24, l_1 = 88, t = 506
            return LinearCoderFactory.getInstance(88).getDatawordBitLength();
        } else if (serverElementSize > (1 << 16)) {
            // 2^16 < n <= 2^20, l_1 = 80, t = 495
            return LinearCoderFactory.getInstance(80).getDatawordBitLength();
        } else if (serverElementSize > (1 << 12)) {
            // 2^12 < n <= 2^16, l_1 = 72, t = 473
            return LinearCoderFactory.getInstance(72).getDatawordBitLength();
        } else {
            // 2^00 < n <= 2^12, l_1 = 64, t = 448
            return LinearCoderFactory.getInstance(64).getDatawordBitLength();
        }
    }

    /**
     * Gets dataword bit length for the binary coder, which depends on the server element size (l_1) and the client
     * element size (l_2). The paper states that:
     * <p>
     * Consider a malicious sender and recall how the simulator extracts an effective input for that sender.
     * We recommend setting l_2 = 2κ in our malicious instantiation.
     * </p>
     * <p>
     * Consider a malicious receiver and recall how the simulator extracts an effective input for that receiver.
     * For reference, we have computed some concrete parameter settings. The values are given in Figure 5. We consider
     * an adversary making q = 2^128 queries to H1.
     * </p>
     * Therefore, we provide this function for choosing dataword bit length for the binary coder.
     *
     * @param serverElementSize server element size.
     * @param clientElementSize client element size.
     * @return LCOT input bit length.
     */
    @SuppressWarnings("AlibabaUndefineMagicConstant")
    private static int getMaliciousCoderDatawordBitLength(EnvType envType, Gf2eDokvsType paxosType,
                                                          int serverElementSize, int clientElementSize) {
        int m = Gf2eDokvsFactory.getM(envType, paxosType, serverElementSize);
        MathPreconditions.checkPositiveInRangeClosed("m (for server element size)", m, 1 << 24);
        MathPreconditions.checkPositiveInRangeClosed("client_element_size", clientElementSize, 1 << 24);
        // n' = client element size
        if (m > (1 << 20)) {
            // 2^20 < m <= 2^24
            if (clientElementSize <= 2 * m) {
                // n' = 2m, l_1 = 209, t = 732
                return LinearCoderFactory.getInstance(209).getDatawordBitLength();
            } else if (clientElementSize <= 3 * m) {
                // n' = 3m, l_1 = 156, t = 627
                return LinearCoderFactory.getInstance(156).getDatawordBitLength();
            } else if (clientElementSize <= 4 * m) {
                // n' = 4m, l_1 = 138, t = 594
                return LinearCoderFactory.getInstance(138).getDatawordBitLength();
            } else {
                // n' = 5m, l_1 = 129, t = 583
                return LinearCoderFactory.getInstance(129).getDatawordBitLength();
            }
        } else if (m > (1 << 16)) {
            // 2^16 < m <= 2^20
            if (clientElementSize <= 2 * m) {
                // n' = 2m, l_1 = 217, t = 744
                return LinearCoderFactory.getInstance(217).getDatawordBitLength();
            } else if (clientElementSize <= 3 * m) {
                // n' = 3m, l_1 = 162, t = 638
                return LinearCoderFactory.getInstance(162).getDatawordBitLength();
            } else if (clientElementSize <= 4 * m) {
                // n' = 4m, l_1 = 144, t = 605
                return LinearCoderFactory.getInstance(144).getDatawordBitLength();
            } else {
                // n' = 5m, l_1 = 134, t = 594
                return LinearCoderFactory.getInstance(134).getDatawordBitLength();
            }
        } else if (m > (1 << 12)) {
            // 2^12 < n <= 2^16
            if (clientElementSize <= 2 * m) {
                // n' = 2m, l_1 = 225, t = 768
                return LinearCoderFactory.getInstance(225).getDatawordBitLength();
            } else if (clientElementSize <= 3 * m) {
                // n' = 3m, l_1 = 168, t = 649
                return LinearCoderFactory.getInstance(168).getDatawordBitLength();
            } else if (clientElementSize <= 4 * m) {
                // n' = 4m, l_1 = 149, t = 616
                return LinearCoderFactory.getInstance(149).getDatawordBitLength();
            } else {
                // n' = 5m, l_1 = 139, t = 605
                return LinearCoderFactory.getInstance(139).getDatawordBitLength();
            }
        } else {
            // 2^00 < n <= 2^12
            if (clientElementSize <= 2 * m) {
                // n' = 2m, l_1 = 233, t = 776
                return LinearCoderFactory.getInstance(233).getDatawordBitLength();
            } else if (clientElementSize <= 3 * m) {
                // n' = 3m, l_1 = 174, t = 660
                return LinearCoderFactory.getInstance(174).getDatawordBitLength();
            } else if (clientElementSize <= 4 * m) {
                // n' = 4m, l_1 = 154, t = 627
                return LinearCoderFactory.getInstance(154).getDatawordBitLength();
            } else {
                // n' = 5m, l_1 = 144, t = 605
                return LinearCoderFactory.getInstance(144).getDatawordBitLength();
            }
        }
    }

    public static int getMaxL(EnvType envType, SecurityModel securityModel,
                              Gf2eDokvsType paxosType, int serverElementSize, int clientElementSize) {
        switch (securityModel) {
            case MALICIOUS:
            case COVERT:
                return getMaliciousCoderDatawordBitLength(envType, paxosType, serverElementSize, clientElementSize);
            case SEMI_HONEST:
            case TRUSTED_DEALER:
            case IDEAL:
                return getSemiHonestCoderDatawordBitLength(serverElementSize, clientElementSize);
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel);
        }
    }
}
