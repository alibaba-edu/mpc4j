package edu.alibaba.mpc4j.common.rpc.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * data packet buffer test.
 *
 * @author Weiran Liu
 * @date 2023/2/9
 */
public class DataPacketBufferTest {
    /**
     * party 1's ID
     */
    static final int PARTY_1_ID = 1;
    /**
     * party 2's ID
     */
    static final int PARTY_2_ID = 2;
    /**
     * start index for party 1's payload list
     */
    static final long LIST_START_INDEX = 0L;
    /**
     * end index for party 1's payload list
     */
    static final long LIST_END_INDEX = 1000L;
    /**
     * start index for party 1's payload set
     */
    static final long SET_START_INDEX = 1000L;
    /**
     * end index for party 1's payloads
     */
    static final long SET_END_INDEX = 2000L;
    /**
     * correct list
     */
    private static final List<Long> CORRECT_LIST = LongStream.range(LIST_START_INDEX, LIST_END_INDEX)
        .boxed()
        .collect(Collectors.toList());
    /**
     * correct set
     */
    private static final Set<Long> CORRECT_SET = LongStream.range(SET_START_INDEX, SET_END_INDEX)
        .boxed()
        .collect(Collectors.toSet());

    @Test
    public void testDataPacketBuffer() throws InterruptedException {
        DataPacketBuffer dataPacketBuffer = new DataPacketBuffer();
        DataPacketBufferParty1Thread party1Thread = new DataPacketBufferParty1Thread(dataPacketBuffer);
        DataPacketBufferParty2Thread party2Thread = new DataPacketBufferParty2Thread(dataPacketBuffer);
        party1Thread.start();
        party2Thread.start();
        party1Thread.join();
        party2Thread.join();

        Assert.assertEquals(CORRECT_LIST, party1Thread.getPayloadList());
        Assert.assertEquals(CORRECT_LIST, party2Thread.getPayloadList());
        Assert.assertEquals(CORRECT_SET, party1Thread.getPayloadSet());
        Assert.assertEquals(CORRECT_SET, party2Thread.getPayloadSet());
    }
}
