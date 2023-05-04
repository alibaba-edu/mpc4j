package edu.alibaba.mpc4j.s2pc.opf.opprf.batch.okvs;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * OKVS Batch OPRRF protocol description. The original scheme is described by instantiating OKVS as a
 * polynomial or MegaBin in the following paper:
 * <p>
 * Pinkas, Benny, Thomas Schneider, Oleksandr Tkachenko, and Avishay Yanai. Efficient circuit-based PSI with linear
 * communication. EUROCRYPT 2019, pp. 122-153. Springer International Publishing, 2019.
 * </p>
 * Then, the following paper shows that the general OKVS can be used to replace polynomial / MegaBin:
 * <p>
 * Garimella, Gayathri, Benny Pinkas, Mike Rosulek, Ni Trieu, and Avishay Yanai. Oblivious key-value stores and
 * amplification for private set intersection. CRYPTO 2021, pp. 395-425. Springer International Publishing, 2021.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
class OkvsBopprfPtoDesc implements PtoDesc {
    /**
     * the protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7202528621691376701L);
    /**
     * the protocol name
     */
    private static final String PTO_NAME = "OKVS_BOPPRF";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * the sender sends OKVS keys
         */
        SENDER_SEND_OKVS_KEYS,
        /**
         * the sender sends OKVS
         */
        SENDER_SEND_OKVS,
    }

    /**
     * the singleton mode
     */
    private static final OkvsBopprfPtoDesc INSTANCE = new OkvsBopprfPtoDesc();

    /**
     * private constructor.
     */
    private OkvsBopprfPtoDesc() {
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
