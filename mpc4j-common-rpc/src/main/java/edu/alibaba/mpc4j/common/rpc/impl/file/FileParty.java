package edu.alibaba.mpc4j.common.rpc.impl.file;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.File;

/**
 * 文件通信参与方信息。
 *
 * @author Weiran Liu
 * @date 2021/12/17
 */
public class FileParty implements Party {
    /**
     * 参与方ID
     */
    private final int partyId;
    /**
     * 参与方名称
     */
    private final String partyName;
    /**
     * 参与方接收数据的文件路径
     */
    private final String partyFilePath;

    /**
     * 构建文件通信参与方信息。
     *
     * @param partyId   参与方ID。
     * @param partyName 参与方名称。
     */
    FileParty(int partyId, String partyName, String partyFilePath) {
        Preconditions.checkArgument(partyId >= 0, "Party ID must be greater than 0");
        Preconditions.checkArgument(StringUtils.isNotBlank(partyName), "Party Name should not be blank");
        File file = new File(partyFilePath);
        Preconditions.checkArgument(file.isDirectory(), "%s must be a path", partyFilePath);
        this.partyId = partyId;
        this.partyName = partyName;
        this.partyFilePath = partyFilePath;
    }

    @Override
    public int getPartyId() {
        return partyId;
    }

    @Override
    public String getPartyName() {
        return partyName;
    }

    public String getPartyFilePath() {
        return partyFilePath;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(partyId)
            .append(partyName)
            .append(partyFilePath)
            .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FileParty)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        FileParty that = (FileParty)obj;
        return new EqualsBuilder()
            .append(this.partyId, that.partyId)
            .append(this.partyName, that.partyName)
            .append(this.partyFilePath, that.partyFilePath)
            .isEquals();
    }

    @Override
    public String toString() {
        return String.format("%s (ID = %s, path = %s)", partyName, partyId, partyFilePath);
    }
}
