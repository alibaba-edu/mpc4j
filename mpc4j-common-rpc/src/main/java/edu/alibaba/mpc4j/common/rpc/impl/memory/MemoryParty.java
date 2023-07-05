package edu.alibaba.mpc4j.common.rpc.impl.memory;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * 内存通信参与方信息。
 *
 * @author Weiran Liu
 * @date 2021/12/09
 */
public class MemoryParty implements Party {
    /**
     * 参与方ID
     */
    private final int partyId;
    /**
     * 参与方名称
     */
    private final String partyName;

    /**
     * 构建内存通信参与方信息。
     *
     * @param partyId   参与方ID。
     * @param partyName 参与方名称。
     */
    public MemoryParty(int partyId, String partyName) {
        Preconditions.checkArgument(partyId >= 0, "Party ID must be greater than 0");
        Preconditions.checkArgument(StringUtils.isNotBlank(partyName), "Party Name should not be blank");
        this.partyId = partyId;
        this.partyName = partyName;
    }

    @Override
    public int getPartyId() {
        return partyId;
    }

    @Override
    public String getPartyName() {
        return partyName;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(partyId)
            .append(partyName)
            .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MemoryParty)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        MemoryParty that = (MemoryParty)obj;
        return new EqualsBuilder()
            .append(this.partyId, that.partyId)
            .append(this.partyName, that.partyName)
            .isEquals();
    }

    @Override
    public String toString() {
        return String.format("%s (ID = %s)", partyName, partyId);
    }
}
