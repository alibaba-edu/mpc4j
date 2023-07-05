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
public class DataPacketBufferParty1Thread extends Thread {
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

    DataPacketBufferParty1Thread(DataPacketBuffer dataPacketBuffer) {
        this.dataPacketBuffer = dataPacketBuffer;
        payloadList = new LinkedList<>();
        payloadSet = new HashSet<>();
    }

    @Override
    public void run() {
        try {
            // put set data
            for (long i = DataPacketBufferTest.SET_START_INDEX; i < DataPacketBufferTest.SET_END_INDEX; i++) {
                DataPacketHeader party1Header = new DataPacketHeader(
                    0, 0, 0, i, DataPacketBufferTest.PARTY_1_ID, DataPacketBufferTest.PARTY_2_ID
                );
                List<byte[]> party1Payload = new LinkedList<>();
                party1Payload.add(LongUtils.longToByteArray(i));
                dataPacketBuffer.put(DataPacket.fromByteArrayList(party1Header, party1Payload));
            }
            // take list data
            for (long j = DataPacketBufferTest.LIST_START_INDEX; j < DataPacketBufferTest.LIST_END_INDEX; j++) {
                DataPacketHeader party2Header = new DataPacketHeader(
                    0, 0, 0, j, DataPacketBufferTest.PARTY_2_ID, DataPacketBufferTest.PARTY_1_ID
                );
                List<byte[]> party2Payload = dataPacketBuffer.take(party2Header).getPayload();
                payloadList.add(LongUtils.byteArrayToLong(party2Payload.remove(0)));
            }
            // take set data
            for (long j = DataPacketBufferTest.SET_START_INDEX; j < DataPacketBufferTest.SET_END_INDEX; j++) {
                List<byte[]> party2Payload = dataPacketBuffer.take(DataPacketBufferTest.PARTY_1_ID).getPayload();
                payloadSet.add(LongUtils.byteArrayToLong(party2Payload.remove(0)));
            }
            // put list data
            for (long i = DataPacketBufferTest.LIST_START_INDEX; i < DataPacketBufferTest.LIST_END_INDEX; i++) {
                DataPacketHeader party1Header = new DataPacketHeader(
                    0, 0, 0, i, DataPacketBufferTest.PARTY_1_ID, DataPacketBufferTest.PARTY_2_ID
                );
                List<byte[]> party1Payload = new LinkedList<>();
                party1Payload.add(LongUtils.longToByteArray(i));
                dataPacketBuffer.put(DataPacket.fromByteArrayList(party1Header, party1Payload));
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
