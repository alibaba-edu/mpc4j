package edu.alibaba.mpc4j.common.rpc.utils;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * The data packet header. The total length is 8 + 4 + 4 + 8 + 4 + 4 = 32 bytes, with the following information:
 * <li>encodeTaskId (long): the first 32 bits is the position in the protocol tree, the last 32 bits is the task ID.</li>
 * <li>ptoId (int): the protocol ID.</li>
 * <li>stepId (int): the step ID.</li>
 * <li>extraInfo: (long): the extra information, like Lamport clock in distribute systems.</li>
 * <li>senderId (int): the sender ID.</li>
 * <li>receiverId (int): the receiver ID.</li>
 *
 * @author Weiran Liu
 * @date 2021/12/08
 */
public class DataPacketHeader {
    /**
     * the encoded task ID. The first 32 bits is the position in the protocol tree, the last 32 bits is the task ID.
     */
    private final long encodeTaskId;
    /**
     * the protocol ID.
     */
    private final int ptoId;
    /**
     * the step ID.
     */
    private final int stepId;
    /**
     * the sender ID.
     */
    private final int senderId;
    /**
     * the receiver ID.
     */
    private final int receiverId;
    /**
     * the extra information.
     */
    private final long extraInfo;

    /**
     * Creates a new data packet header. The extraInfo is set to 0L.
     *
     * @param encodeTaskId the encoded task ID.
     * @param ptoId        the protocol ID.
     * @param stepId       the step ID.
     * @param senderId     the sender ID.
     * @param receiverId   the receiver ID.
     */
    public DataPacketHeader(long encodeTaskId, int ptoId, int stepId, int senderId, int receiverId) {
        this(encodeTaskId, ptoId, stepId, 0L, senderId, receiverId);
    }

    /**
     * Creates a new data packet header.
     *
     * @param encodeTaskId the encoded task ID.
     * @param ptoId        the protocol ID.
     * @param stepId       the step ID.
     * @param extraInfo    the extra information, like Lamport clock in distribute systems.
     * @param senderId     the sender ID.
     * @param receiverId   the receiver ID.
     */
    public DataPacketHeader(long encodeTaskId, int ptoId, int stepId, long extraInfo, int senderId, int receiverId) {
        MathPreconditions.checkNonNegative("encodeTaskId", encodeTaskId);
        MathPreconditions.checkNonNegative("ptoId", ptoId);
        MathPreconditions.checkNonNegative("stepId", stepId);
        MathPreconditions.checkNonNegative("extraInfo", extraInfo);
        MathPreconditions.checkNonNegative("senderId", senderId);
        MathPreconditions.checkNonNegative("receiverId", receiverId);
        assert receiverId >= 0;
        this.encodeTaskId = encodeTaskId;
        this.ptoId = ptoId;
        this.stepId = stepId;
        this.extraInfo = extraInfo;
        this.senderId = senderId;
        this.receiverId = receiverId;
    }

    /**
     * Gets the encoded task ID.
     *
     * @return the encoded task ID.
     */
    public long getEncodeTaskId() {
        return encodeTaskId;
    }

    /**
     * Gets the protocol ID.
     *
     * @return the protocol ID.
     */
    public int getPtoId() {
        return ptoId;
    }

    /**
     * Gets the step ID.
     *
     * @return the step ID.
     */
    public int getStepId() {
        return stepId;
    }

    /**
     * Gets the extra information.
     *
     * @return the extra information.
     */
    public long getExtraInfo() {
        return extraInfo;
    }

    /**
     * Gets the sender ID.
     *
     * @return the sender ID.
     */
    public int getSenderId() {
        return senderId;
    }

    /**
     * Gets the receiver ID.
     *
     * @return the receiver ID.
     */
    public int getReceiverId() {
        return receiverId;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(encodeTaskId)
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
        DataPacketHeader that = (DataPacketHeader) obj;
        return new EqualsBuilder()
            .append(this.encodeTaskId, that.encodeTaskId)
            .append(this.ptoId, that.ptoId)
            .append(this.stepId, that.stepId)
            .append(this.extraInfo, that.extraInfo)
            .append(this.senderId, that.senderId)
            .append(this.receiverId, that.receiverId)
            .isEquals();
    }
}
