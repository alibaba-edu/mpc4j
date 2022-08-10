package edu.alibaba.mpc4j.common.tool.crypto.kdf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory.KdfType;

/**
 * 本地Blake2b密钥派生函数。
 *
 * @author Weiran Liu
 * @date 2021/12/31
 */
class NativeBlake2bKdf implements Kdf {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_TOOL_NAME);
    }

    @Override
    public byte[] deriveKey(byte[] seed) {
        assert seed.length > 0;

        return nativeDeriveKey(seed);
    }

    /**
     * 本地派生密钥。
     *
     * @param seed 种子。
     * @return 派生密钥。
     */
    private native byte[] nativeDeriveKey(byte[] seed);

    @Override
    public KdfType getKdfType() {
        return KdfType.NATIVE_BLAKE_2B;
    }
}
