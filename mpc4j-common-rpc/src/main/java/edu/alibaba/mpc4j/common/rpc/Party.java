package edu.alibaba.mpc4j.common.rpc;

/**
 * 协议参与方接口。
 *
 * @author Weiran Liu
 * @date 2021/12/08
 */
public interface Party extends Comparable<Party> {
    /**
     * 返回参与方ID。
     *
     * @return 参与方ID。
     */
    int getPartyId();

    /**
     * 返回参与方名称。
     *
     * @return 参与方名称。
     */
    String getPartyName();

    @Override
    default int compareTo(Party otherPartySpec) {
        return Integer.compare(getPartyId(), otherPartySpec.getPartyId());
    }
}
