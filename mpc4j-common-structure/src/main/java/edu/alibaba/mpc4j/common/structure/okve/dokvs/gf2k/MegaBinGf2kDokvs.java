package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2k;

import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2k.Gf2kDokvsFactory.Gf2kDokvsType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.MegaBinGf2eDokvs;

import java.util.Map;

/**
 * Mega-Bin GF2K DOKVS.
 *
 * @author Weiran Liu
 * @date 2023/7/26
 */
class MegaBinGf2kDokvs<T> implements Gf2kDokvs<T> {
    /**
     * GF2K type
     */
    private static final Gf2kDokvsType GF2K_TYPE = Gf2kDokvsType.MEGA_BIN;
    /**
     * GF2E-DOKVS
     */
    private final MegaBinGf2eDokvs<T> gf2eDokvs;

    MegaBinGf2kDokvs(EnvType envType, int n, byte[][] keys) {
        gf2eDokvs = new MegaBinGf2eDokvs<>(envType, n, CommonConstants.BLOCK_BIT_LENGTH, keys);
    }


    @Override
    public Gf2kDokvsType getType() {
        return GF2K_TYPE;
    }

    @Override
    public void setParallelEncode(boolean parallelEncode) {
        gf2eDokvs.setParallelEncode(parallelEncode);
    }

    @Override
    public boolean getParallelEncode() {
        return gf2eDokvs.getParallelEncode();
    }

    @Override
    public byte[][] encode(Map<T, byte[]> keyValueMap, boolean doublyEncode) throws ArithmeticException {
        return gf2eDokvs.encode(keyValueMap, doublyEncode);
    }

    @Override
    public byte[] decode(byte[][] storage, T key) {
        return gf2eDokvs.decode(storage, key);
    }

    @Override
    public int getN() {
        return gf2eDokvs.getN();
    }

    @Override
    public int getM() {
        return gf2eDokvs.getM();
    }
}
