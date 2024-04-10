package edu.alibaba.mpc4j.s2pc.upso.upsi;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * abstract UPSI server.
 *
 * @author Liqiang Peng
 * @date 2022/6/14
 */
public abstract class AbstractUpsiServer<T> extends AbstractTwoPartyPto implements UpsiServer<T> {
    /**
     * max client element size
     */
    private int maxClientElementSize;
    /**
     * server element list
     */
    protected List<ByteBuffer> serverElementList;
    /**
     * byte array object map
     */
    protected Map<ByteBuffer, T> byteArrayObjectMap;
    /**
     * server element size
     */
    protected int serverElementSize;
    /**
     * client element size
     */
    protected int clientElementSize;
    /**
     * bot element bytebuffer
     */
    protected ByteBuffer botElementByteBuffer;

    protected AbstractUpsiServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, UpsiConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(int maxClientElementSize) {
        MathPreconditions.checkPositive("maxClientElementSize", maxClientElementSize);
        this.maxClientElementSize = maxClientElementSize;
        initState();
    }

    protected void setPtoInput(Set<T> serverElementSet, int clientElementSize) {
        checkInitialized();
        byte[] botElementByteArray = new byte[CommonConstants.STATS_BYTE_LENGTH];
        Arrays.fill(botElementByteArray, (byte)0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        MathPreconditions.checkPositive("serverElementSize", serverElementSet.size());
        serverElementList = serverElementSet.stream()
            .map(ObjectUtils::objectToByteArray)
            .map(ByteBuffer::wrap)
            .peek(xi -> Preconditions.checkArgument(!xi.equals(botElementByteBuffer), "xi must not equal ⊥"))
            .collect(Collectors.toCollection(ArrayList::new));
        serverElementSize = serverElementSet.size();
        MathPreconditions.checkPositiveInRangeClosed("clientElementSize", clientElementSize, maxClientElementSize);
        this.clientElementSize = clientElementSize;
        byteArrayObjectMap = new HashMap<>(clientElementSize);
        serverElementSet.forEach(serverElement ->
            byteArrayObjectMap.put(ByteBuffer.wrap(ObjectUtils.objectToByteArray(serverElement)), serverElement)
        );
        extraInfo++;
    }
}