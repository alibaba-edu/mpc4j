package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;

import java.util.List;

/**
 * abstract two-party aid protocol.
 *
 * @author Weiran Liu
 * @date 2024/6/10
 */
public abstract class AbstractTwoPartyAidPto extends AbstractThreePartyPto implements TwoPartyAidPto {
    /**
     * aid party extra info
     */
    protected long aidPartyExtraInfo;

    /**
     * Creates a two-party aid protocol.
     *
     * @param ptoDesc    protocol description.
     * @param ownRpc     own RPC.
     * @param leftParty  left party.
     * @param rightParty right party.
     * @param config     config.
     */
    protected AbstractTwoPartyAidPto(PtoDesc ptoDesc, Rpc ownRpc, Party leftParty, Party rightParty,
                                     MultiPartyPtoConfig config) {
        super(ptoDesc, ownRpc, leftParty, rightParty, config);
    }

    /**
     * Receives any aid data packet from a party.
     *
     * @return any aid data packet.
     */
    protected DataPacket receiveAnyAidDataPacket() {
        DataPacket dataPacket = rpc.receiveAny(ptoDesc.getPtoId());
        DataPacketHeader header = dataPacket.getHeader();
        int senderId = header.getSenderId();
        aidPartyExtraInfo = header.getExtraInfo();
        assert senderId == leftParty().getPartyId() || senderId == rightParty().getPartyId();
        return dataPacket;
    }

    /**
     * Receives any aid data packet from the other party base on the received header.
     *
     * @param receivedHeader received header.
     * @return that data packet.
     */
    protected DataPacket receiveAnyAidDataPacket(DataPacketHeader receivedHeader) {
        long encodeTaskId = receivedHeader.getEncodeTaskId();
        int ptoId = receivedHeader.getPtoId();
        assert ptoId == getPtoDesc().getPtoId();
        int stepId = receivedHeader.getStepId();
        long extraInfo = receivedHeader.getExtraInfo();
        assert aidPartyExtraInfo == extraInfo;
        int thisId = receivedHeader.getSenderId();
        assert thisId == leftParty().getPartyId() || thisId == rightParty().getPartyId();
        // receive request query from that party
        int thatId = (thisId == leftParty().getPartyId() ? rightParty().getPartyId() : leftParty().getPartyId());
        DataPacketHeader thatHeader = new DataPacketHeader(
            encodeTaskId, ptoId, stepId, extraInfo, thatId, ownParty().getPartyId()
        );
        return rpc.receive(thatHeader);
    }

    /**
     * Sends aid payload to the left party according to the received header.
     *
     * @param encodeTaskId encode task ID.
     * @param stepId       step ID.
     * @param payload      payload.
     */
    protected void sendLeftPartyAidPayload(long encodeTaskId, int stepId, List<byte[]> payload) {
        DataPacketHeader leftHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), stepId, aidPartyExtraInfo,
            ownParty().getPartyId(), leftParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(leftHeader, payload));
    }

    /**
     * Sends payload to the right party according ot the received header.
     *
     * @param encodeTaskId encode task ID.
     * @param stepId       step ID.
     * @param payload      payload.
     */
    protected void sendRightPartyAidPayload(long encodeTaskId, int stepId, List<byte[]> payload) {
        DataPacketHeader rightHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), stepId, aidPartyExtraInfo,
            ownParty().getPartyId(), rightParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(rightHeader, payload));
    }

    /**
     * Sends payload to left party.
     *
     * @param stepId  step ID.
     * @param payload payload.
     */
    protected void sendLeftPartyPayload(int stepId, List<byte[]> payload) {
        sendPayload(stepId, leftParty(), payload);
    }

    /**
     * Receives payload from left party.
     *
     * @param stepId step ID.
     * @return payload.
     */
    protected List<byte[]> receiveLeftPartyPayload(int stepId) {
        return receivePayload(stepId, leftParty());
    }

    /**
     * Sends payload to right party.
     *
     * @param stepId  step ID.
     * @param payload payload.
     */
    protected void sendRightPartyPayload(int stepId, List<byte[]> payload) {
        sendPayload(stepId, rightParty(), payload);
    }

    /**
     * Receives payload from left party.
     *
     * @param stepId step ID.
     * @return payload.
     */
    protected List<byte[]> receiveRightPartyPayload(int stepId) {
        return receivePayload(stepId, rightParty());
    }
}
