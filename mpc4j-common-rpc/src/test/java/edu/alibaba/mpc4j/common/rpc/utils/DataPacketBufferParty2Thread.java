package edu.alibaba.mpc4j.common.rpc.utils;

import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * data packet buffer party 1 thread.
 *
 * @author Weiran Liu
 * @date 2023/2/9
 */
public class DataPacketBufferParty2Thread extends Thread {
    /**
     * data packet buffer
     */
    private final DataPacketBuffer dataPacketBuffer;
    /**
     * sequence payloads
     */
    private final List<Long> payloadList;
    /**
     * set payloads
     */
    private final Set<Long> payloadSet;

    DataPacketBufferParty2Thread(DataPacketBuffer dataPacketBuffer) {
        this.dataPacketBuffer = dataPacketBuffer;
        payloadList = new LinkedList<>();
        payloadSet = new HashSet<>();
    }

    @Override
    public void run() {
        try {
            // put list data
            for (long j = DataPacketBufferTest.LIST_START_INDEX; j < DataPacketBufferTest.LIST_END_INDEX; j++) {
                DataPacketHeader party2Header = new DataPacketHeader(
                    0, 0, 0, j, DataPacketBufferTest.PARTY_2_ID, DataPacketBufferTest.PARTY_1_ID
                );
                List<byte[]> party2Payload = new LinkedList<>();
                party2Payload.add(LongUtils.longToByteArray(j));
                dataPacketBuffer.put(DataPacket.fromByteArrayList(party2Header, party2Payload));
            }
            // take set data
            for (long i = DataPacketBufferTest.SET_START_INDEX; i < DataPacketBufferTest.SET_END_INDEX; i++) {
                List<byte[]> party1Payload = dataPacketBuffer.take(DataPacketBufferTest.PARTY_2_ID).getPayload();
                payloadSet.add(LongUtils.byteArrayToLong(party1Payload.remove(0)));
            }
            // put set data
            for (long j = DataPacketBufferTest.SET_START_INDEX; j < DataPacketBufferTest.SET_END_INDEX; j++) {
                DataPacketHeader party2Header = new DataPacketHeader(
                    0, 0, 0, j, DataPacketBufferTest.PARTY_2_ID, DataPacketBufferTest.PARTY_1_ID
                );
                List<byte[]> party2Payload = new LinkedList<>();
                party2Payload.add(LongUtils.longToByteArray(j));
                dataPacketBuffer.put(DataPacket.fromByteArrayList(party2Header, party2Payload));
            }
            // take list data
            for (long i = DataPacketBufferTest.LIST_START_INDEX; i < DataPacketBufferTest.LIST_END_INDEX; i++) {
                DataPacketHeader party1Header = new DataPacketHeader(
                    0, 0, 0, i, DataPacketBufferTest.PARTY_1_ID, DataPacketBufferTest.PARTY_2_ID
                );
                List<byte[]> party1Payload = dataPacketBuffer.take(party1Header).getPayload();
                payloadList.add(LongUtils.byteArrayToLong(party1Payload.remove(0)));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public List<Long> getPayloadList() {
        return payloadList;
    }

    public Set<Long> getPayloadSet() {
        return payloadSet;
    }
}
