package edu.alibaba.mpc4j.dp.service.fo.hadamard;

import edu.alibaba.mpc4j.common.tool.coder.linear.HadamardCoder;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.util.Random;

/**
 * Hadamard Response (HR) Frequency Oracle LDP client. This is the standard HR mechanism implementation. See paper:
 * <p>
 * Acharya, Jayadev, Ziteng Sun, and Huanyu Zhang. Hadamard response: Estimating distributions privately, efficiently,
 * and with little communication. AISATAS 2019, pp. 1120-1129. PMLR, 2019.
 * </p>
 * Although the standard HR mechanism is more suitable for low ε, we do not restrict ε to be very low.
 *
 * @author Weiran Liu
 * @date 2023/1/19
 */
public class HrFoLdpClient extends AbstractFoLdpClient {
    /**
     * the Hadamard matrix dataword bit length, which also equals to the output bit length.
     */
    private final int k;
    /**
     * the Hadamard matrix size, the smallest exponent of 2 that is bigger than d, which also equals to the output size.
     */
    private final int n;
    /**
     * p = e^ε / (e^ε + 1)
     */
    private final double p;

    public HrFoLdpClient(FoLdpConfig config) {
        super(config);
        // the smallest exponent of 2 which is bigger than d
        k = LongUtils.ceilLog2(d + 1);
        n = 1 << k;
        p = 1 - 1.0 / (1 + Math.exp(epsilon));
    }

    @Override
    public byte[] randomize(String item, Random random) {
        checkItemInDomain(item);
        int itemIndex = domain.getItemIndex(item);
        // map the input symbol x to x + 1 since we are not using the first column of the matrix
        int x = itemIndex + 1;
        // get a random number in [0, n) as y1
        int y1 = random.nextInt(n);
        // construct y2 so that if H[x][y1] = 0, then H[x][y2] = 1, and if H[x][y1] = 1, then H[x][y2] = 0
        // If we flip a 1 position for y1, then the result must be flipped,
        int y2 = -1;
        for (int i = 0; i < k; i++) {
            if (IntUtils.getLittleEndianBoolean(x, i)) {
                y2 = y1 ^ (1 << i);
                break;
            }
        }
        assert y2 >= 0 && y2 < n : "y2 must be in range [0, " + n + ")";
        // check if H[x][y1] = 1
        boolean y1Check = HadamardCoder.checkParity(x, y1);
        double u = random.nextDouble();
        if (y1Check) {
            // if H[x][y1] = 1, output y1 with probability p
            if (u < p) {
                return IntUtils.boundedNonNegIntToByteArray(y1, n);
            } else {
                return IntUtils.boundedNonNegIntToByteArray(y2, n);
            }
        } else {
            // if H[x][y1] = 0, it must be that H[x][y2] = 1, output y2 with probability p
            if (u < p) {
                return IntUtils.boundedNonNegIntToByteArray(y2, n);
            } else {
                return IntUtils.boundedNonNegIntToByteArray(y1, n);
            }
        }
    }
}
