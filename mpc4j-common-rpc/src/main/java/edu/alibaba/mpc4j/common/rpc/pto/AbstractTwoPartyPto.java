package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;

import java.util.List;

/**
 * Abstract two-party protocol.
 *
 * @author Weiran Liu
 * @date 2022/4/28
 */
public abstract class AbstractTwoPartyPto extends AbstractMultiPartyPto implements TwoPartyPto {
    /**
     * Constructs a two party protocol.
     *
     * @param ptoDesc    protocol description.
     * @param ownRpc     own RPC.
     * @param otherParty other party.
     * @param config     config.
     */
    protected AbstractTwoPartyPto(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, MultiPartyPtoConfig config) {
        super(ptoDesc, config, ownRpc, otherParty);
    }

    /**
     * Constructs a two party protocol.
     *
     * @param ptoDesc    protocol description.
     * @param ownRpc     own RPC.
     * @param otherParty other party.
     * @param aidParty   aid party.
     * @param config     config.
     */
    protected AbstractTwoPartyPto(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, Party aidParty,
                                  MultiPartyPtoConfig config) {
        super(ptoDesc, config, ownRpc, otherParty, aidParty);
    }

    /**
     * Sends payload to the other party.
     *
     * @param stepId  step ID.
     * @param payload payload.
     */
    protected void sendOtherPartyPayload(int stepId, List<byte[]> payload) {
        sendPayload(stepId, otherParty(), payload);
    }

    /**
     * Sends payload to the other party.
     *
     * @param stepId  step ID.
     * @param payload payload.
     */
    protected void sendOtherPartyEqualSizePayload(int stepId, List<byte[]> payload) {
        sendEqualSizePayload(stepId, otherParty(), payload);
    }

    /**
     * Receives payload from the other party.
     *
     * @param stepId step ID.
     * @return payload.
     */
    protected List<byte[]> receiveOtherPartyPayload(int stepId) {
        return receivePayload(stepId, otherParty());
    }

    /**
     * Receives payload from the other party, used in the protocols that single message may exceed 1GB
     *
     * @param stepId step ID.
     * @param num the number of arrays in the list
     * @param byteLength the byte length of each array
     * @return payload.
     */
    protected List<byte[]> receiveOtherPartyEqualSizePayload(int stepId, int num, int byteLength) {
        return receiveEqualSizePayload(stepId, otherParty(), num, byteLength);
    }

    /**
     * Gets aid party.
     *
     * @return aid party.
     */
    private Party aidParty() {
        return otherParties()[1];
    }

    /**
     * Sends payload to aid party.
     *
     * @param stepId  step ID.
     * @param payload payload.
     */
    protected void sendAidPartyPayload(int stepId, List<byte[]> payload) {
        sendPayload(stepId, aidParty(), payload);
    }

    /**
     * Receives payload from the aid party.
     *
     * @param stepId step ID.
     * @return payload.
     */
    protected List<byte[]> receiveAiderPayload(int stepId) {
        return receivePayload(stepId, aidParty());
    }
}
