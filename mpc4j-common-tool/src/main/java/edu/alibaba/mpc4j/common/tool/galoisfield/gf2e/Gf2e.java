package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import edu.alibaba.mpc4j.common.tool.galoisfield.BytesField;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;

import java.util.Arrays;

/**
 * GF(2^l) interface.
 *
 * @author Weiran Liu
 * @date 2022/4/27
 */
public interface Gf2e extends BytesField {
    /**
     * Gets the Gf2e type.
     *
     * @return the Gf2e type.
     */
    Gf2eType getGf2eType();

    /**
     * Gets the name.
     *
     * @return the name.
     */
    @Override
    default String getName() {
        return getGf2eType().name();
    }
}
