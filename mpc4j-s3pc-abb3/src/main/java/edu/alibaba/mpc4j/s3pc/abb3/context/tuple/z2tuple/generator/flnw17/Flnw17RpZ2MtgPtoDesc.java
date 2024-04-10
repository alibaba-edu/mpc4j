package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.generator.flnw17;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * z2 mtg for 3pc description. The protocol is described in the following paper:
 * <p>
 * Jun Furukawa, Yehuda Lindell, Ariel Nof, and Or Weinstein.
 * High-Throughput Secure Three-Party Computation for Malicious Adversaries and an Honest Majority
 * EUROCRYPT 2017 pp. 225–255, 2017.
 * </p>
 *
 * @author Feng Han
 * @date 2024/01/03
 */
public class Flnw17RpZ2MtgPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7950463109150080514L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "FLNW+17_RP_Z2MTG";

    /**
     * singleton mode
     */
    private static final Flnw17RpZ2MtgPtoDesc INSTANCE = new Flnw17RpZ2MtgPtoDesc();

    /**
     * private constructor
     */
    private Flnw17RpZ2MtgPtoDesc() {
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
