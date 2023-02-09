package edu.alibaba.mpc4j.s2pc.pjc.pid;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.PmidPartyOutput;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PID服务端/客户端输出。
 *
 * @author Weiran Liu
 * @date 2022/01/19
 */
public class PidPartyOutput<T> {
    /**
     * PID字节长度
     */
    private final int pidByteLength;
    /**
     * PID集合
     */
    private final Set<ByteBuffer> pidSet;
    /**
     * ID映射
     */
    private final Map<ByteBuffer, T> pidMap;

    /**
     * 构造PID服务端输出。
     *
     * @param pidSet PID集合、
     * @param pidMap ID映射。
     */
    public PidPartyOutput(int pidByteLength, Set<ByteBuffer> pidSet, Map<ByteBuffer, T> pidMap) {
        assert pidByteLength >= CommonConstants.STATS_BYTE_LENGTH;
        this.pidByteLength = pidByteLength;
        assert pidSet.size() > 0;
        // 验证ID映射中的PID都在PID集合中
        for (ByteBuffer pid : pidMap.keySet()) {
            assert pidSet.contains(pid);
        }
        this.pidSet = pidSet.stream()
            .peek(pid -> {
                assert pid.array().length == pidByteLength;
            })
            .map(pid -> ByteBuffer.wrap(BytesUtils.clone(pid.array())))
            .collect(Collectors.toSet());
        this.pidMap = pidMap.keySet().stream()
            .collect(Collectors.toMap(pid -> ByteBuffer.wrap(BytesUtils.clone(pid.array())), pidMap::get));
    }

    public PidPartyOutput(PmidPartyOutput<T> pmidPartyOutput) {
        pidByteLength = pmidPartyOutput.getPmidByteLength();
        pidSet = pmidPartyOutput.getPmidSet();
        pidMap = pmidPartyOutput.getPmidMap();
        // 验证k最大为1
        long nonDistinctCount = pidMap.keySet().stream().map(pidMap::get).filter(Objects::nonNull).count();
        long distinctCount = pmidPartyOutput.getIdSet().size();
        assert nonDistinctCount == distinctCount : "PmidMap should not contain duplicate ID";
    }

    /**
     * 返回PID集合。
     *
     * @return PID集合。
     */
    public Set<ByteBuffer> getPidSet() {
        return pidSet;
    }

    /**
     * 返回ID集合。
     *
     * @return ID集合。
     */
    public Set<T> getIdSet() {
        return pidMap.keySet().stream().map(pidMap::get).collect(Collectors.toSet());
    }

    /**
     * 返回PID所对应的ID。
     *
     * @param pid 输入的PID。
     * @return 对应的ID，如果没有对应的结果，则返回{@code null}。
     */
    public T getId(ByteBuffer pid) {
        return pidMap.get(pid);
    }

    public int getPidByteLength() {
        return pidByteLength;
    }
}
