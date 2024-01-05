package edu.alibaba.mpc4j.common.structure.filter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bouncycastle.util.Arrays;

import java.nio.ByteBuffer;

/**
 * entry in cuckoo filter.
 *
 * @author Weiran Liu
 * @date 2020/08/30
 */
public class CuckooFilterEntry {
    /**
     * bit length for each fingerprint, computed as (log_2(1/ε) + log_2(2 * ENTRIES_PER_BUCKET)).
     * Since ENTRIES_PER_BUCKET = 4, log_2(1/ε) = 40, the result is 40 + 3 = 43, we round to 48, see Table 2.
     */
    static final int FINGERPRINT_BYTE_LENGTH = 6;
    /**
     * all-zero fingerprint
     */
    static final ByteBuffer ZERO_FINGERPRINT = ByteBuffer.wrap(new byte[FINGERPRINT_BYTE_LENGTH]);
    /**
     * 插入元素所对应的指纹，长度参见{@code CuckooFilter.FINGERPRINT_BYTE_LENGTH}
     */
    private final ByteBuffer fingerprint;

    /**
     * Creates an assigned cuckoo filter entry.
     *
     * @param fingerPrint fingerprint.
     */
    CuckooFilterEntry(ByteBuffer fingerPrint) {
        byte[] data = fingerPrint.array();
        MathPreconditions.checkEqual("expect length", "actual length", FINGERPRINT_BYTE_LENGTH, data.length);
        // we do not allow all-zero fingerprint
        Preconditions.checkArgument(!Arrays.areAllZeroes(data, 0, data.length));
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
