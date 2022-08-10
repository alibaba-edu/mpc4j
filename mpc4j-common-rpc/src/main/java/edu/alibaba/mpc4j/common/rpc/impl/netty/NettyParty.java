package edu.alibaba.mpc4j.common.rpc.impl.netty;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Netty网络通信参与方。
 *
 * @author Weiran Liu
 * @date 2020/10/11
 */
public class NettyParty implements Party {
    /**
     * 参与方ID
     */
    private final int partyId;
    /**
     * 参与方名称
     */
    private final String partyName;
    /**
     * 参与方IP地址
     */
    private final String host;
    /**
     * 参与方端口
     */
    private final int port;

    public NettyParty(int partyId, String partyName, String host, int port) {
        Preconditions.checkArgument(partyId >= 0, "Party ID must be greater than 0");
        Preconditions.checkArgument(StringUtils.isNotBlank(partyName), "Party Name should not be blank");
        Preconditions.checkArgument(StringUtils.isNotBlank(host), "Host IP address should not be blank");
        Preconditions.checkArgument(port > 0, "Host port should be greater than 0");
        this.partyId = partyId;
        this.partyName = partyName;
        this.host = host;
        this.port = port;
    }

    @Override
    public int getPartyId() {
        return partyId;
    }

    @Override
    public String getPartyName() {
        return partyName;
    }

    /**
     * 返回参与方主机地址。
     *
     * @return 参与方主机地址。
     */
    public String getHost() {
        return host;
    }

    /**
     * 返回参与方端口。
     *
     * @return 参与方端口。
     */
    public int getPort() {
        return port;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(partyId)
            .append(partyName)
            .append(host)
            .append(port)
            .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NettyParty)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        NettyParty that = (NettyParty)obj;
        return new EqualsBuilder()
            .append(this.partyId, that.partyId)
            .append(this.partyName, that.partyName)
            .append(this.host, that.host)
            .append(this.port, that.port)
            .isEquals();
    }

    @Override
    public String toString() {
        return String.format("%s (ID = %s, Host = %s, Port = %s)", partyName, partyId, host, port);
    }
}
