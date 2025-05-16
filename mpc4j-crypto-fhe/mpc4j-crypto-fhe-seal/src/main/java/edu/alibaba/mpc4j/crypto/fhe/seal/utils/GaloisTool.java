package edu.alibaba.mpc4j.crypto.fhe.seal.utils;

/**
 * Galois operation tool class.
 * <p>
 * The implementation is from
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/galois.h">galois.h</a>
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/11
 */
public class GaloisTool extends AbstractGaloisTool {
    /**
     * Creates a Galois tool. This is the original SEAL implementation where generator = 3.
     *
     * @param coeffCountPower k such that n = 2^k.
     */
    public GaloisTool(int coeffCountPower) {
        super(coeffCountPower, 3);
    }
}