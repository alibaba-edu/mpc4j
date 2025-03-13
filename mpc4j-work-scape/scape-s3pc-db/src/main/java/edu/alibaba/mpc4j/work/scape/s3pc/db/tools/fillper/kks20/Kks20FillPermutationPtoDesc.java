package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.kks20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * fill the incomplete permutation using butterfly net
 * The scheme comes from the following paper:
 *
 * <p>
 * Simeon Krastnikov; Florian Kerschbaum; Douglas Stebila
 * Efficient Oblivious Database Joins
 * VLDB 2020
 * </p>
 *
 * @author Feng Han
 * @date 2025/2/17
 */
public class Kks20FillPermutationPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -4549449270699906384L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "BUTTERFLY_NET_KKS20";

    /**
     * singleton mode
     */
    private static final Kks20FillPermutationPtoDesc INSTANCE = new Kks20FillPermutationPtoDesc();

    /**
     * private constructor
     */
    private Kks20FillPermutationPtoDesc() {
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
