package edu.alibaba.mpc4j.s2pc.opf.osn;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.util.Vector;
import java.util.stream.Collectors;

/**
 * OSN参与方输出。
 *
 * @author Weiran Liu
 * @date 2022/02/09
 */
public class OsnPartyOutput {
    /**
     * 秘密分享向量
     */
    private final Vector<byte[]> shareVector;
    /**
     * 秘密分享字节长度
     */
    private final int shareByteLength;

    public OsnPartyOutput(int shareByteLength, Vector<byte[]> shareVector) {
        assert shareByteLength >= CommonConstants.STATS_BYTE_LENGTH;
        this.shareByteLength = shareByteLength;
        assert shareVector.size() > 1;
        this.shareVector = shareVector.stream()
            .peek(share -> {
                assert share.length == shareByteLength;
            })
            .map(BytesUtils::clone)
            .collect(Collectors.toCollection(Vector::new));
    }

    /**
     * 返回分享值。
     *
     * @param index 索引值。
     * @return 分享值。
     */
    public byte[] getShare(int index) {
        return shareVector.get(index);
    }

    /**
     * 返回分享值字节长度。
     *
     * @return 分享值字节长度。
     */
    public int getByteLength() {
        return shareByteLength;
    }

    /**
     * 返回向量长度。
     *
     * @return 向量长度。
     */
    public int getN() {
        return shareVector.size();
    }
}
