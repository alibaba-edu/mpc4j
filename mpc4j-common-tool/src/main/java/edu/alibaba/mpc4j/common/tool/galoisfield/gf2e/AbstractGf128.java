package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * abstract GF(2^128).
 *
 * @author Weiran Liu
 * @date 2024/6/4
 */
abstract class AbstractGf128 extends AbstractGf2e {

    AbstractGf128(EnvType envType) {
        super(envType, 128);
    }

    @Override
    public boolean validateElement(byte[] p) {
        return p.length == byteL;
    }
}
