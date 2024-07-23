package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.mul;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * MulPIR protocol description. The protocol comes from the following paper:
 * <p>
 * A. Ali and T. Lepoint and S. Patel and M. Raykova and P. Schoppmann and K. Seth and K. Yeo
 * Communication-Computation Trade-offs in PIR.
 * In 2021 USENIX Security Symposium. 2021, 1811-1828.
 * <p/>
 * The implementation is based on <a href="https://github.com/OpenMined/PIR">...</a>.
 *
 * @author Qixian Zhou
 * @date 2023/5/29
 */
public class MulStdIdxPirPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 1559646295196399549L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "MUL_PIR";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * client sends public keys
         */
        CLIENT_SEND_PUBLIC_KEYS,
        /**
         * client send query
         */
        CLIENT_SEND_QUERY,
        /**
         * server send response
         */
        SERVER_SEND_RESPONSE,
    }

    /**
     * the singleton mode
     */
    private static final MulStdIdxPirPtoDesc INSTANCE = new MulStdIdxPirPtoDesc();

    /**
     * private constructor.
     */
    private MulStdIdxPirPtoDesc() {
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
