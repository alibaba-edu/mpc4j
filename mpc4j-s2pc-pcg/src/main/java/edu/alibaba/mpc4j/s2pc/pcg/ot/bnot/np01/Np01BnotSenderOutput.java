package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np01;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.AbstractBnotSenderOutput;
import org.bouncycastle.math.ec.ECPoint;

import java.nio.ByteBuffer;


/**
 * NP01-基础N选1-OT协议发送方输出
 *
 * @author Hanwen Feng
 * @date 2022/07/26
 */
public class Np01BnotSenderOutput extends AbstractBnotSenderOutput {
    /**
     * KDF
     */
    private final Kdf kdf;
    /**
     * 群元素的数组[rC_1, rC_1, ..., rC_{n-1}]
     */
    private final ECPoint[] capitalC2rArray;
    /**
     * 群元素数组[rPK_{0,1}, rPK_{0,1}, ..., rPK_{0,num}]
     */
    private final ECPoint[] pk2rArray;

    public Np01BnotSenderOutput(EnvType envType, int n, int num, ECPoint[] capitalC2rArray, ECPoint[] pk2rArray) {
        init(n, num);
        assert (n - 1) == capitalC2rArray.length;
        assert num == pk2rArray.length;
        this.capitalC2rArray = capitalC2rArray;
        this.pk2rArray = pk2rArray;
        kdf = KdfFactory.createInstance(envType);
    }

    @Override
    public byte[] getRb(int index, int choice) {
        assertValidInput(index, choice);
        ECPoint kPoint = (choice == 0) ? pk2rArray[index] : capitalC2rArray[choice - 1].subtract(pk2rArray[index]);
        byte[] kInputArray = kPoint.getEncoded(false);
        return kdf.deriveKey(ByteBuffer
                .allocate(Integer.BYTES + kInputArray.length)
                .putInt(index).put(kInputArray)
                .array()
        );
    }
}



