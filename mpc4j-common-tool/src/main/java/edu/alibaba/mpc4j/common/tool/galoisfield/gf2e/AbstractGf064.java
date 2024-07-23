package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * abstract GF(2^64).
 *
 * @author Weiran Liu
 * @date 2023/8/28
 */
abstract class AbstractGf064 extends AbstractGf2e {

    public AbstractGf064(EnvType envType) {
        super(envType, 64);
    }

    @Override
    public boolean validateElement(byte[] p) {
        return p.length == byteL;
    }
}
