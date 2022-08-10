package edu.alibaba.mpc4j.common.tool.filter;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.nio.ByteBuffer;

/**
 * 存储在布谷鸟过滤器中的条目。
 *
 * @author Weiran Liu
 * @date 2020/08/30
 */
public class CuckooFilterEntry {
    /**
     * 插入元素所对应的指纹，长度参见{@code CuckooFilter.FINGERPRINT_BYTE_LENGTH}
     */
    private final ByteBuffer fingerprint;

    CuckooFilterEntry(ByteBuffer fingerPrint) {
        assert fingerPrint.array().length == CuckooFilter.FINGERPRINT_BYTE_LENGTH;
        this.fingerprint = fingerPrint;
    }

    ByteBuffer getFingerprint() {
        return fingerprint;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(fingerprint).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CuckooFilterEntry)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        CuckooFilterEntry that = (CuckooFilterEntry) obj;
        return new EqualsBuilder().append(this.fingerprint, that.fingerprint).isEquals();
    }
}
