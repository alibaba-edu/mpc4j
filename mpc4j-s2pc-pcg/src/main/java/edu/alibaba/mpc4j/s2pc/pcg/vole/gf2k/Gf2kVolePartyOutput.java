package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k;

import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;

/**
 * GF2K-VOLE output.
 *
 * @author Weiran Liu
 * @date 2024/5/30
 */
public interface Gf2kVolePartyOutput {
    /**
     * Gets field.
     *
     * @return field.
     */
    Sgf2k getField();

    /**
     * Gets subfield.
     *
     * @return subfield.
     */
    default Gf2e getSubfield() {
        return getField().getSubfield();
    }
}
