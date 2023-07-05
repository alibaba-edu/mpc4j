package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import edu.alibaba.mpc4j.common.tool.galoisfield.BytesField;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory.Gf2kType;

/**
 * GF(2^128)有限域运算接口。
 *
 * @author Weiran Liu
 * @date 2022/01/15
 */
public interface Gf2k extends BytesField {
    /**
     * Gets Gf2k type.
     *
     * @return the gf2k type.
     */
    Gf2kType getGf2kType();

    /**
     * Gets the name.
     *
     * @return the name.
     */
    @Override
    default String getName() {
        return getGf2kType().name();
    }
}
