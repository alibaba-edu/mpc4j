package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k;

import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;

/**
 * GF2K-VODE output. VODE is the short for Vector Oblivious Direct Evaluation.
 *
 * @author Weiran Liu
 * @date 2024/6/11
 */
public interface Gf2kVodePartyOutput {
    /**
     * Gets field.
     *
     * @return field.
     */
    Dgf2k getField();

    /**
     * Gets subfield.
     *
     * @return subfield.
     */
    default Gf2e getSubfield() {
        return getField().getSubfield();
    }
}
