package edu.alibaba.mpc4j.dp.service.fo.lh;

import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.hash.IntHashFactory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Binary Local Hash (BLH) Frequency Oracle LDP client. See Section 4.4 of the paper:
 * <p>
 * Wang, Tianhao, and Jeremiah Blocki. Locally differentially private protocols for frequency estimation.
 * USENIX Security 2017.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/1/17
 */
public class BlhFoLdpClient extends AbstractFoLdpClient {
    /**
     * IntHash
     */
    private final IntHash intHash;
    /**
     * p = e^ε / (e^ε + 1)
     */
    private final double p;
    /**
     * q = 1 / (e^ε + 1)
     */
    private final double q;

    public BlhFoLdpClient(FoLdpConfig config) {
        super(config);
        double expEpsilon = Math.exp(epsilon);
        p = expEpsilon / (expEpsilon + 1);
        q = 1 / (expEpsilon + 1);
        intHash = IntHashFactory.fastestInstance();
    }

    @Override
    public byte[] randomize(String item, Random random) {
        checkItemInDomain(item);
        ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES + 1);
        // Encode(v) = <H, b>, where H is chosen uniformly at random, and b = H(v).
        int seed = random.nextInt();
        byteBuffer.putInt(seed);
        byte[] itemIndexBytes = IntUtils.intToByteArray(domain.getItemIndex(item));
        byte b = (byte) (Math.abs(intHash.hash(itemIndexBytes, seed)) % 2);
        // Perturb b to b'
        double u = random.nextDouble();
        if (b == 1) {
            // if b = 1, Pr[b' = 1] = p
            if (u < p) {
                byteBuffer.put((byte) 0x01);
            } else {
                byteBuffer.put((byte) 0x00);
            }
        } else {
            // if b = 0, Pr[b' = 1] = q
            if (u < q) {
                byteBuffer.put((byte) 0x01);
            } else {
                byteBuffer.put((byte) 0x00);
            }
        }
        return byteBuffer.array();
    }
}
