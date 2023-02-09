package edu.alibaba.mpc4j.s2pc.pjc.pmid;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PMID协议输出。
 *
 * @author Weiran Liu
 * @date 2022/5/6
 */
public class PmidPartyOutput<T> {
    /**
     * PMID字节长度
     */
    private final int pmidByteLength;
    /**
     * PMID集合
     */
    private final Set<ByteBuffer> pmidSet;
    /**
     * ID映射
     */
    private final Map<ByteBuffer, T> pmidMap;

    /**
     * 构造PMID服务端输出。
     *
     * @param pmidByteLength PMID字节长度。
     * @param pmidSet PID集合。
     * @param pmidMap ID映射。
     */
    public PmidPartyOutput(int pmidByteLength, Set<ByteBuffer> pmidSet, Map<ByteBuffer, T> pmidMap) {
        assert pmidByteLength >= CommonConstants.STATS_BYTE_LENGTH;
        this.pmidByteLength = pmidByteLength;
        assert pmidSet.size() > 0;
        // 验证ID映射中的PID都在PID集合中
        for (ByteBuffer pmid : pmidMap.keySet()) {
            assert pmidSet.contains(pmid);
        }
        this.pmidSet = pmidSet.stream()
            .peek(pmid -> {
                assert pmid.array().length == pmidByteLength;
            })
            .map(pmid -> ByteBuffer.wrap(BytesUtils.clone(pmid.array())))
            .collect(Collectors.toSet());
        this.pmidMap = pmidMap.keySet().stream()
            .collect(Collectors.toMap(pmid -> ByteBuffer.wrap(BytesUtils.clone(pmid.array())), pmidMap::get));
    }

    /**
     * 返回PMID集合。
     *
     * @return PMID集合。
     */
    public Set<ByteBuffer> getPmidSet() {
        return pmidSet;
    }

    /**
     * 返回ID集合。
     *
     * @return ID集合。
     */
    public Set<T> getIdSet() {
        return pmidMap.keySet().stream().map(pmidMap::get).collect(Collectors.toSet());
    }

    /**
     * 返回PMID所对应的ID。
     *
     * @param pmid 输入的PMID。
     * @return 对应的ID，如果没有对应的结果，则返回{@code null}。
     */
    public T getId(ByteBuffer pmid) {
        return pmidMap.get(pmid);
    }

    /**
     * 返回PMID映射表。
     *
     * @return PMID映射表。
     */
    public Map<ByteBuffer, T> getPmidMap() {
        return pmidMap;
    }

    public int getPmidByteLength() {
        return pmidByteLength;
    }
}
