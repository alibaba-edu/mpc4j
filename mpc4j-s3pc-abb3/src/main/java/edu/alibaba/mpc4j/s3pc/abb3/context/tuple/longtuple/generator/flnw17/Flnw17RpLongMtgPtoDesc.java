package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.generator.flnw17;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * zl64 mtg for 3pc description. The protocol is described in the following paper:
 * <p>
 * Jun Furukawa, Yehuda Lindell, Ariel Nof, and Or Weinstein.
 * High-Throughput Secure Three-Party Computation for Malicious Adversaries and an Honest Majority
 * EUROCRYPT 2017 pp. 225–255, 2017.
 * </p>
 *
 * @author Feng Han
 * @date 2024/01/25
 */
public class Flnw17RpLongMtgPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -6184309486539925727L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "FLNW+17_RP_ZL64MTG";

    /**
     * singleton mode
     */
    private static final Flnw17RpLongMtgPtoDesc INSTANCE = new Flnw17RpLongMtgPtoDesc();

    /**
     * private constructor
     */
    private Flnw17RpLongMtgPtoDesc() {
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
