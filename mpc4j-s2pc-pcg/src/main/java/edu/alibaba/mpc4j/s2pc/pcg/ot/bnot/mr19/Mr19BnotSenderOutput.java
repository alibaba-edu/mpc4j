package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19;

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
 * MR19-Base N选1 OT协议发送方输出
 *
 * @author Hanwen Feng
 * @date 2022/07/26
 */
public class Mr19BnotSenderOutput extends AbstractBnotSenderOutput {
    /**
     * KDF
     */
    private final Kdf kdf;
    /**
     * 私钥b
     */
    private final BigInteger bInteger;
    /**
     * 椭圆曲线
     */
    private final Ecc ecc;
    /**
     * 椭圆曲线点长度
     */
    private final int ecPointByteLength;
    /**
     * ri
     */
    private final ECPoint[] rArray;

    public Mr19BnotSenderOutput(EnvType envType, int n, int num, BigInteger bInteger, ECPoint[] rArray) {
        init(n, num);
        assert rArray.length == n * num;
        this.rArray = rArray;
        this.bInteger = bInteger;
        kdf = KdfFactory.createInstance(envType);
        ecc = EccFactory.createInstance(envType);
        ecPointByteLength = ecc.getG().getEncoded(false).length;
    }

    @Override
    public byte[] getRb(int index, int choice) {
        assertValidInput(index, choice);
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + ecPointByteLength * (n - 1));
        buffer.putInt(choice);
        for (int i = 0; i < n; i++) {
            if (i != choice) {
                buffer.put(rArray[index * n + i].getEncoded(false));
            }
        }
        ECPoint hashPoint = ecc.hashToCurve(buffer.array());
        byte[] kInputArray = ecc.encode(ecc.multiply(rArray[index * n + choice].add(hashPoint), bInteger), false);
        byte[] kInputByteArray = ByteBuffer
            .allocate(Integer.BYTES + kInputArray.length)
            .putInt(index)
            .put(kInputArray)
            .array();
        return kdf.deriveKey(kInputByteArray);
    }
}



