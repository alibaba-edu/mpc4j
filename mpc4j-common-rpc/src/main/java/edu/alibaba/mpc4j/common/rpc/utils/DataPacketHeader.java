package edu.alibaba.mpc4j.common.rpc.utils;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * 数据包头，总长度为8 + 4 + 4 + 4 + 4 + 8 = 32字节，包含下述信息：
 * - taskId: 任务ID，long类型（8字节）。
 * - PtoId：协议ID，int类型（4字节），使用协议描述类生成的serialVersionUID模Integer.MAX_VALUE的结果。
 * - StepId: 步骤ID，int类型（4字节），即步骤枚举值所对应的索引值。
 * - SenderId：发送方ID，int类型（4字节）。
 * - ReceiverId：接收方ID，int类型（4字节）。
 * - extraInfo：额外信息，如门电路ID，long类型（8字节）。
 *
 * @author Weiran Liu
 * @date 2021/12/08
 */
public class DataPacketHeader {
    /**
     * 任务ID
     */
    private final long taskId;
    /**
     * 协议ID
     */
    private final int ptoId;
    /**
     * 步骤ID
     */
    private final int stepId;
    /**
     * 发送方ID
     */
    private final int senderId;
    /**
     * 接收方ID
     */
    private final int receiverId;
    /**
     * 补充信息
     */
    private final long extraInfo;

    /**
     * 构造数据包头。
     *
     * @param taskId     任务ID。
     * @param ptoId      协议ID。
     * @param stepId     步骤ID。
     * @param senderId   发送方ID。
     * @param receiverId 接收方ID。
     */
    public DataPacketHeader(long taskId, int ptoId, int stepId, int senderId, int receiverId) {
        this(taskId, ptoId, stepId, 0L, senderId, receiverId);
    }

    /**
     * 构造数据包头。
     *
     * @param taskId     任务ID。
     * @param ptoId      协议ID。
     * @param stepId     步骤ID。
     * @param senderId   发送方ID。
     * @param receiverId 接收方ID。
     * @param extraInfo  补充信息。
     */
    public DataPacketHeader(long taskId, int ptoId, int stepId, long extraInfo, int senderId, int receiverId) {
        assert taskId >= 0;
        assert ptoId >= 0;
        assert stepId >= 0;
        assert extraInfo >= 0;
        assert senderId >= 0;
        assert receiverId >= 0;
        this.taskId = taskId;
        this.ptoId = ptoId;
        this.stepId = stepId;
        this.extraInfo = extraInfo;
        this.senderId = senderId;
        this.receiverId = receiverId;
    }

    /**
     * 返回任务ID。
     *
     * @return 任务ID。
     */
    public long getTaskId() {
        return taskId;
    }

    /**
     * 返回协议ID。
     *
     * @return 协议ID。
     */
    public int getPtoId() {
        return ptoId;
    }

    /**
     * 返回步骤ID。
     *
     * @return 步骤ID。
     */
    public int getStepId() {
        return stepId;
    }

    /**
     * 返回额外信息。
     *
     * @return 额外信息。
     */
    public long getExtraInfo() {
        return extraInfo;
    }

    /**
     * 返回发送方ID。
     *
     * @return 发送方ID。
     */
    public int getSenderId() {
        return senderId;
    }

    /**
     * 返回接收方ID。
     *
     * @return 接收方ID。
     */
    public int getReceiverId() {
        return receiverId;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(taskId)
            .append(ptoId)
            .append(stepId)
            .append(extraInfo)
            .append(senderId)
            .append(receiverId)
            .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DataPacketHeader)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        DataPacketHeader that = (DataPacketHeader)obj;
        return new EqualsBuilder()
            .append(this.taskId, that.taskId)
            .append(this.ptoId, that.ptoId)
            .append(this.stepId, that.stepId)
            .append(this.extraInfo, that.extraInfo)
            .append(this.senderId, that.senderId)
            .append(this.receiverId, that.receiverId)
            .isEquals();
    }
}
