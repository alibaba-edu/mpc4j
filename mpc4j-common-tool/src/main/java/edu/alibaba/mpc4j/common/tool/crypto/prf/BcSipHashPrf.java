package edu.alibaba.mpc4j.common.tool.crypto.prf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.bouncycastle.crypto.macs.SipHash;
import org.bouncycastle.crypto.params.KeyParameter;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * 使用Bouncy Castle中SipHash实现的PRF。
 *
 * @author Weiran Liu
 * @date 2021/12/13
 */
class BcSipHashPrf implements Prf {
    /**
     * 单位输出长度
     */
    private static final int UNIT_BYTE_LENGTH = Long.BYTES;
    /**
     * 伪随机数生成器
     */
    private final Prg prg;
    /**
     * 输出字节长度
     */
    private final int outputByteLength;
    /**
     * 密钥
     */
    private byte[] key;
    /**
     * 为了保证线程安全，每次执行时都要初始化新的SipHash并重新设置密钥，因此要重新设置
     */
    private KeyParameter keyParameter;

    BcSipHashPrf(int outputByteLength) {
        this.outputByteLength = outputByteLength;
        prg = PrgFactory.createInstance(PrgFactory.PrgType.JDK_AES_ECB, outputByteLength);
    }

    @Override
    public int getOutputByteLength() {
        return outputByteLength;
    }

    @Override
    public void setKey(byte[] key) {
        assert key.length == CommonConstants.BLOCK_BYTE_LENGTH;
        // 内部代码会把密钥拷贝一份，但这里外部还需要拷贝一次，使得返回的key和输入的key是一致的
        keyParameter = new KeyParameter(key);
        this.key = BytesUtils.clone(key);
    }

    @Override
    public byte[] getKey() {
        return key;
    }

    @Override
    public byte[] getBytes(byte[] message) {
        assert message.length > 0;
        assert keyParameter != null;
        SipHash sipHash = new SipHash();
        sipHash.init(keyParameter);
        if (outputByteLength <= UNIT_BYTE_LENGTH) {
            byte[] leftInput = ByteBuffer.allocate(message.length + 1)
                .put(message)
                .put((byte)0x00)
                .array();
            // 如果输出字节长度等于MAC单位输出长度，则只调用一次MAC函数
            sipHash.update(leftInput, 0, leftInput.length);
            byte[] leftMac = ByteBuffer.allocate(Long.BYTES)
                .putLong(sipHash.doFinal())
                .array();
            if (outputByteLength == UNIT_BYTE_LENGTH) {
                return leftMac;
            } else {
                return Arrays.copyOf(leftMac, outputByteLength);
            }
        }
        ByteBuffer macByteBuffer = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH);
        byte[] leftInput = ByteBuffer.allocate(message.length + 1)
            .put(message)
            .put((byte)0x01)
            .array();
        sipHash.update(leftInput, 0, leftInput.length);
        macByteBuffer.putLong(sipHash.doFinal());
        byte[] rightInput = ByteBuffer.allocate(message.length + 1)
            .put(message)
            .put((byte)0x02)
            .array();
        sipHash.update(rightInput, 0, rightInput.length);
        macByteBuffer.putLong(sipHash.doFinal());
        byte[] mac = macByteBuffer.array();
        if (outputByteLength == CommonConstants.BLOCK_BYTE_LENGTH) {
            return mac;
        } else if (outputByteLength < CommonConstants.BLOCK_BYTE_LENGTH) {
            return Arrays.copyOf(mac, outputByteLength);
        } else {
            return prg.extendToBytes(mac);
        }
    }

    @Override
    public PrfFactory.PrfType getPrfType() {
        return PrfFactory.PrfType.BC_SIP_HASH;
    }
}
