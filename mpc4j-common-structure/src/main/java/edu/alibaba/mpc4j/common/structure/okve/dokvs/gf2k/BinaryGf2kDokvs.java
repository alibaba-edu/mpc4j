package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2k;

import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2k.Gf2kDokvsFactory.Gf2kDokvsType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;

import java.security.SecureRandom;
import java.util.Map;

/**
 * abstract binary GF2K DOKVS.
 *
 * @author Weiran Liu
 * @date 2023/7/11
 */
class BinaryGf2kDokvs<T> extends AbstractGf2kDokvs<T> implements Gf2kDokvs<T> {
    /**
     * type
     */
    private final Gf2kDokvsType type;
    /**
     * GF2E-DOKVS
     */
    private final Gf2eDokvs<T> gf2eDokvs;

    BinaryGf2kDokvs(EnvType envType, Gf2kDokvsType type, Gf2eDokvsType gf2eDokvsType, int n, byte[][] keys) {
        this(envType, type, gf2eDokvsType, n, keys, new SecureRandom());
    }

    BinaryGf2kDokvs(EnvType envType, Gf2kDokvsType type, Gf2eDokvsType gf2eDokvsType, int n, byte[][] keys, SecureRandom secureRandom) {
        super(envType, n, Gf2eDokvsFactory.getM(envType, gf2eDokvsType, n), secureRandom);
        this.type = type;
        gf2eDokvs = Gf2eDokvsFactory.createInstance(envType, gf2eDokvsType, n, CommonConstants.BLOCK_BIT_LENGTH, keys);
    }


    @Override
    public Gf2kDokvsType getType() {
        return type;
    }

    @Override
    public void setParallelEncode(boolean parallelEncode) {
        super.setParallelEncode(parallelEncode);
        gf2eDokvs.setParallelEncode(parallelEncode);
    }

    @Override
    public byte[][] encode(Map<T, byte[]> keyValueMap, boolean doublyEncode) throws ArithmeticException {
        return gf2eDokvs.encode(keyValueMap, doublyEncode);
    }

    @Override
    public byte[] decode(byte[][] storage, T key) {
        return gf2eDokvs.decode(storage, key);
    }
}
