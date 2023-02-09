package edu.alibaba.mpc4j.s2pc.pir.index;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.IntStream;

/**
 * 索引PIR协议服务端抽象类。
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public abstract class AbstractIndexPirServer extends AbstractSecureTwoPartyPto implements IndexPirServer {
    /**
     * 配置项
     */
    private final IndexPirConfig config;
    /**
     * 服务端元素字节数组
     */
    protected ArrayList<byte[][]> elementByteArray = new ArrayList<>();
    /**
     * 服务端元素数量
     */
    protected int num;
    /**
     * 元素字节长度
     */
    protected int elementByteLength;

    protected AbstractIndexPirServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, IndexPirConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
        this.config = config;
    }

    @Override
    public IndexPirFactory.IndexPirType getPtoType() {
        return config.getProType();
    }

    protected void setInitInput(ArrayList<ByteBuffer> elementArrayList, int elementByteLength, int binMaxByteLength,
                                String protocolName) {
        assert elementByteLength > 0 : "element byte length must be greater than 0: " + elementByteLength;
        this.elementByteLength = elementByteLength;
        assert elementArrayList.size() > 0 : "num must be greater than 0";
        num = elementArrayList.size();
        IntStream.range(0, num).forEach(index -> {
            byte[] element = elementArrayList.get(index).array();
            assert element.length == elementByteLength :
                "element byte length must be " + elementByteLength + ": " + element.length;
            assert !protocolName.equals(IndexPirFactory.IndexPirType.FAST_PIR.name()) || elementByteLength % 2 == 0;
        });
        // 分块数量
        int binNum = (elementByteLength + binMaxByteLength - 1) / binMaxByteLength;
        int lastBinByteLength = elementByteLength % binMaxByteLength == 0 ?
            binMaxByteLength : elementByteLength % binMaxByteLength;
        for (int i = 0; i < binNum; i++) {
            int byteLength = i == binNum - 1 ? lastBinByteLength : binMaxByteLength;
            byte[][] byteArray = new byte[num][byteLength];
            for (int j = 0; j < num; j++) {
                System.arraycopy(elementArrayList.get(j).array(), i * binMaxByteLength, byteArray[j], 0, byteLength);
            }
            elementByteArray.add(byteArray);
        }
        extraInfo++;
        initialized = false;
    }

    protected void setPtoInput() {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        extraInfo++;
    }

    /**
     * 将字节数组转换为指定比特长度的long型数组。
     *
     * @param limit     long型数值的比特长度。
     * @param offset    移位。
     * @param size      待转换的字节数组长度。
     * @param byteArray 字节数组。
     * @return long型数组。
     */
    protected long[] convertBytesToCoeffs(int limit, int offset, int size, byte[] byteArray) {
        // 需要使用的系数个数
        int longArraySize = (int) Math.ceil(Byte.SIZE * size / (double) limit);
        long[] longArray = new long[longArraySize];
        int room = limit;
        int flag = 0;
        for (int i = 0; i < size; i++) {
            int src = byteArray[i+offset];
            if (src < 0) {
                src &= 0xFF;
            }
            int rest = Byte.SIZE;
            while (rest != 0) {
                if (room == 0) {
                    flag++;
                    room = limit;
                }
                int shift = Math.min(room, rest);
                long temp = longArray[flag] << shift;
                longArray[flag] = temp | (src >> (Byte.SIZE - shift));
                int remain = (1 << (Byte.SIZE - shift)) - 1;
                src = (src & remain) << shift;
                room -= shift;
                rest -= shift;
            }
        }
        longArray[flag] = longArray[flag] << room;
        return longArray;
    }
}