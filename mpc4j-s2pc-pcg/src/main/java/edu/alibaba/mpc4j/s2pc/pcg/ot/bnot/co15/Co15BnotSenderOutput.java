package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.co15;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.AbstractBnotSenderOutput;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;


/**
 * CO15-Base N选1 OT协议发送方输出
 *
 * @author Hanwen Feng
 * @date 2022/07/25
 */
public class Co15BnotSenderOutput extends AbstractBnotSenderOutput {
    /**
     * KDF
     */
    private final Kdf kdf;
    /**
     * 群元素的数组R
     */
    private final ECPoint[] capitalRArray;
    /**
     * 群元素T
     */
    private final ECPoint capitalT;
    /**
     * 群元素数组[0T, 1T, ..., nT]
     */
    private final ECPoint[] capitalTArray;
    /**
     * 椭圆曲线
     */
    private final Ecc ecc;

    public Co15BnotSenderOutput(int n, int num, ECPoint[] capitalRs, ECPoint capitalT, EnvType envType) {
        init(n, num);
        assert num == capitalRs.length;
        this.capitalRArray = capitalRs;
        this.capitalT = capitalT;
        this.kdf = KdfFactory.createInstance(envType);
        this.ecc = EccFactory.createInstance(envType);
        capitalTArray = new ECPoint[n];
    }

    @Override
    public byte[] getRb(int index, int choice) {
        assertValidInput(index, choice);
        if (capitalTArray[choice] == null) {
            capitalTArray[choice] = ecc.multiply(capitalT, BigInteger.valueOf(choice));
        }
        byte[] kInputArray = ecc.encode(capitalRArray[index].subtract(capitalTArray[choice]), false);
        return kdf.deriveKey(ByteBuffer
                .allocate( Integer.BYTES + kInputArray.length)
                .putInt(index)
                .put(kInputArray)
                .array()
        );
    }
}



