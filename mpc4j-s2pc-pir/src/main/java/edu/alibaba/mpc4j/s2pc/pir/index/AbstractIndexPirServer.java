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
    protected byte[] elementByteArray;
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

    protected void setInitInput(ArrayList<ByteBuffer> elementArrayList, int elementByteLength) {
        assert elementByteLength > 0 : "element byte length must be greater than 0: " + elementByteLength;
        this.elementByteLength = elementByteLength;
        assert elementArrayList.size() > 0 : "num must be greater than 0";
        num = elementArrayList.size();
        // 将元素打平
        elementByteArray = new byte[num * elementByteLength];
        IntStream.range(0, num).forEach(index -> {
            byte[] element = elementArrayList.get(index).array();
            assert element.length == elementByteLength
                : "element byte length must be " + elementByteLength + ": " + element.length;
            System.arraycopy(element, 0, elementByteArray, index * elementByteLength, elementByteLength);
        });
        extraInfo++;
        initialized = false;
    }

    protected void setPtoInput() {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        extraInfo++;
    }
}
