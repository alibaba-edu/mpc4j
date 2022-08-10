package edu.alibaba.mpc4j.common.tool.crypto.prf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.bouncycastle.crypto.macs.SipHash128;
import org.bouncycastle.crypto.params.KeyParameter;

import java.util.Arrays;

/**
 * 使用Bouncy Castle中Sip128Hash实现的PRF。
 *
 * @author Weiran Liu
 * @date 2021/12/13
 */
class BcSip128HashPrf implements Prf {
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
     * 为了保证线程安全，每次执行时都要初始化新的Sip128Hash并重新设置密钥，因此要重新设置
     */
    private KeyParameter keyParameter;

    BcSip128HashPrf(int outputByteLength) {
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
        SipHash128 sip128Hash = new SipHash128();
        sip128Hash.init(keyParameter);
        byte[] mac = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        sip128Hash.update(message, 0, message.length);
        sip128Hash.doFinal(mac, 0);
        if (outputByteLength == CommonConstants.BLOCK_BYTE_LENGTH) {
            // 如果输出字节长度等于MAC单位输出长度，则直接输出
            return mac;
        } else if (outputByteLength < CommonConstants.BLOCK_BYTE_LENGTH) {
            // 如果输出字节长度小于MAC单位输出长度，则截断输出
            return Arrays.copyOf(mac, outputByteLength);
        } else {
            // 如果输出字节长度大于MAC单位输出长度，则PRG扩展
            return prg.extendToBytes(mac);
        }
    }

    @Override
    public PrfFactory.PrfType getPrfType() {
        return PrfFactory.PrfType.BC_SIP128_HASH;
    }
}
