package edu.alibaba.mpc4j.common.tool.galoisfield.gf64;

import edu.alibaba.mpc4j.common.tool.galoisfield.BytesField;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf64.Gf64Factory.Gf64Type;

/**
 * GF(2^64) finite field.
 *
 * @author Weiran Liu
 * @date 2023/8/28
 */
public interface Gf64 extends BytesField {
    /**
     * Gets type.
     *
     * @return type.
     */
    Gf64Type getGf64Type();

    /**
     * Gets name.
     *
     * @return name.
     */
    @Override
    default String getName() {
        return getGf64Type().name();
    }
}
