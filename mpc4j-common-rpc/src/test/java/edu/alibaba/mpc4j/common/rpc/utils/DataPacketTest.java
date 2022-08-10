package edu.alibaba.mpc4j.common.rpc.utils;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.RpcTestUtils;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据包测试。
 *
 * @author Weiran Liu
 * @date 2021/12/09
 */
public class DataPacketTest {
    /**
     * 发送方
     */
    private final Party sender;
    /**
     * 接收方
     */
    private final Party receiver;

    public DataPacketTest() {
        RpcManager rpcManager = new MemoryRpcManager(2);
        sender = rpcManager.getRpc(0).ownParty();
        receiver = rpcManager.getRpc(1).ownParty();
    }

    @Test
    public void testEmptyDataPacket() {
        // 生成第一个数据包
        List<byte[]> emptyData1 = new LinkedList<>();
        DataPacketHeader emptyHeader1 = new DataPacketHeader(
            0L, DataPacketTestPtoDesc.getInstance().getPtoId(), DataPacketTestPtoDesc.PtoStep.EMPTY.ordinal(),
            sender.getPartyId(), receiver.getPartyId()
        );
        DataPacket emptyDataPacket1 = DataPacket.fromByteArrayList(emptyHeader1, emptyData1);
        // 生成第二个数据包
        List<byte[]> emptyData2 = new LinkedList<>();
        DataPacketHeader emptyHeader2 = new DataPacketHeader(
            0L, DataPacketTestPtoDesc.getInstance().getPtoId(), DataPacketTestPtoDesc.PtoStep.EMPTY.ordinal(),
            sender.getPartyId(), receiver.getPartyId()
        );
        DataPacket emptyDataPacket2 = DataPacket.fromByteArrayList(emptyHeader2, emptyData2);

        Assert.assertEquals(emptyDataPacket1, emptyDataPacket2);
    }

    @Test
    public void testIntDataPacket() {
        // 生成第一个数据包
        List<byte[]> intArrayData1 = Arrays.stream(RpcTestUtils.INT_ARRAY)
            .mapToObj(value -> ByteBuffer.allocate(Integer.BYTES).putInt(value).array())
            .collect(Collectors.toCollection(LinkedList::new));
        DataPacketHeader intArrayHeader1 = new DataPacketHeader(
            0L, DataPacketTestPtoDesc.getInstance().getPtoId(), DataPacketTestPtoDesc.PtoStep.INTEGER.ordinal(),
            sender.getPartyId(), receiver.getPartyId()
        );
        DataPacket intArrayDataPacket1 = DataPacket.fromByteArrayList(intArrayHeader1, intArrayData1);
        // 生成第二个数据包
        List<byte[]> intArrayData2 = Arrays.stream(RpcTestUtils.INT_ARRAY)
            .mapToObj(value -> ByteBuffer.allocate(Integer.BYTES).putInt(value).array())
            .collect(Collectors.toCollection(LinkedList::new));
        DataPacketHeader intArrayHeader2 = new DataPacketHeader(
            0L, DataPacketTestPtoDesc.getInstance().getPtoId(), DataPacketTestPtoDesc.PtoStep.INTEGER.ordinal(),
            sender.getPartyId(), receiver.getPartyId()
        );
        DataPacket intArrayDataPacket2 = DataPacket.fromByteArrayList(intArrayHeader2, intArrayData2);

        Assert.assertEquals(intArrayDataPacket1, intArrayDataPacket2);
        int[] intArray = intArrayDataPacket1.getPayload().stream()
            .mapToInt(data -> ByteBuffer.wrap(data).getInt())
            .toArray();
        Assert.assertArrayEquals(RpcTestUtils.INT_ARRAY, intArray);
    }

    @Test
    public void testDoubleDataPacket() {
        // 生成第一个数据包
        List<byte[]> doubleArrayData1 = Arrays.stream(RpcTestUtils.DOUBLE_ARRAY)
            .mapToObj(value -> ByteBuffer.allocate(Double.BYTES).putDouble(value).array())
            .collect(Collectors.toCollection(LinkedList::new));
        DataPacketHeader doubleArrayHeader1 = new DataPacketHeader(
            0L, DataPacketTestPtoDesc.getInstance().getPtoId(), DataPacketTestPtoDesc.PtoStep.DOUBLE.ordinal(),
            sender.getPartyId(), receiver.getPartyId()
        );
        DataPacket doubleArrayDataPacket1 = DataPacket.fromByteArrayList(doubleArrayHeader1, doubleArrayData1);
        // 生成第二个数据包
        List<byte[]> doubleArrayData2 = Arrays.stream(RpcTestUtils.DOUBLE_ARRAY)
            .mapToObj(value -> ByteBuffer.allocate(Double.BYTES).putDouble(value).array())
            .collect(Collectors.toCollection(LinkedList::new));
        DataPacketHeader doubleArrayHeader2 = new DataPacketHeader(
            0L, DataPacketTestPtoDesc.getInstance().getPtoId(), DataPacketTestPtoDesc.PtoStep.DOUBLE.ordinal(),
            sender.getPartyId(), receiver.getPartyId()
        );
        DataPacket doubleArrayDataPacket2 = DataPacket.fromByteArrayList(doubleArrayHeader2, doubleArrayData2);

        Assert.assertEquals(doubleArrayDataPacket1, doubleArrayDataPacket2);
        double[] doubleArray = doubleArrayDataPacket1.getPayload().stream()
            .mapToDouble(data -> ByteBuffer.wrap(data).getDouble())
            .toArray();
        Assert.assertArrayEquals(RpcTestUtils.DOUBLE_ARRAY, doubleArray, 1e-7);
    }

    @Test
    public void testByteArrayDataPacket() {
        // 生成第一个数据包
        List<byte[]> byteArrayData1 = Arrays.stream(RpcTestUtils.BYTES_ARRAY)
            .collect(Collectors.toCollection(LinkedList::new));
        DataPacketHeader byteArrayHeader1 = new DataPacketHeader(
            0L, DataPacketTestPtoDesc.getInstance().getPtoId(), DataPacketTestPtoDesc.PtoStep.BYTE_ARRAY.ordinal(),
            sender.getPartyId(), receiver.getPartyId()
        );
        DataPacket byteArrayDataPacket1 = DataPacket.fromByteArrayList(byteArrayHeader1, byteArrayData1);
        // 生成第二个数据包
        List<byte[]> byteArrayData2 = Arrays.stream(RpcTestUtils.BYTES_ARRAY)
            .collect(Collectors.toCollection(LinkedList::new));
        DataPacketHeader byteArrayHeader2 = new DataPacketHeader(
            0L, DataPacketTestPtoDesc.getInstance().getPtoId(), DataPacketTestPtoDesc.PtoStep.BYTE_ARRAY.ordinal(),
            sender.getPartyId(), receiver.getPartyId()
        );
        DataPacket byteArrayDataPacket2 = DataPacket.fromByteArrayList(byteArrayHeader2, byteArrayData2);

        Assert.assertEquals(byteArrayDataPacket1, byteArrayDataPacket2);
        byte[][] bytesArray = byteArrayDataPacket1.getPayload().toArray(new byte[0][]);
        Assert.assertArrayEquals(RpcTestUtils.BYTES_ARRAY, bytesArray);
    }

    @Test
    public void testBigIntegerDataPacket() {
        // 生成第一个数据包
        List<byte[]> bigIntegerData1 = Arrays.stream(RpcTestUtils.BIGINTEGER_ARRAY)
            .map(BigInteger::toByteArray)
            .collect(Collectors.toCollection(LinkedList::new));
        DataPacketHeader bigIntegerHeader1 = new DataPacketHeader(
            0L, DataPacketTestPtoDesc.getInstance().getPtoId(), DataPacketTestPtoDesc.PtoStep.BIGINTEGER.ordinal(),
            sender.getPartyId(), receiver.getPartyId()
        );
        DataPacket bigIntegerDataPacket1 = DataPacket.fromByteArrayList(bigIntegerHeader1, bigIntegerData1);
        // 生成第二个数据包
        List<byte[]> bigIntegerData2 = Arrays.stream(RpcTestUtils.BIGINTEGER_ARRAY)
            .map(BigInteger::toByteArray)
            .collect(Collectors.toCollection(LinkedList::new));
        DataPacketHeader bigIntegerHeader2 = new DataPacketHeader(
            0L, DataPacketTestPtoDesc.getInstance().getPtoId(), DataPacketTestPtoDesc.PtoStep.BIGINTEGER.ordinal(),
            sender.getPartyId(), receiver.getPartyId()
        );
        DataPacket bigIntegerDataPacket2 = DataPacket.fromByteArrayList(bigIntegerHeader2, bigIntegerData2);

        Assert.assertEquals(bigIntegerDataPacket1, bigIntegerDataPacket2);
        // 读取结果应该相同
        BigInteger[] bigIntegerArray = bigIntegerDataPacket1.getPayload().stream()
            .map(BigInteger::new)
            .toArray(BigInteger[]::new);
        Assert.assertArrayEquals(RpcTestUtils.BIGINTEGER_ARRAY, bigIntegerArray);
    }
}
