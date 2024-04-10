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
 * abstract UPSI client.
 *
 * @author Liqiang Peng
 * @date 2022/6/13
 */
public abstract class AbstractUpsiClient<T> extends AbstractTwoPartyPto implements UpsiClient<T> {
    /**
     * max client element size
     */
    private int maxClientElementSize;
    /**
     * client element list
     */
    protected List<ByteBuffer> clientElementList;
    /**
     * byte array object map
     */
    protected Map<ByteBuffer, T> byteArrayObjectMap;
    /**
     * client element size
     */
    protected int clientElementSize;
    /**
     * bot element bytebuffer
     */
    protected ByteBuffer botElementByteBuffer;

    protected AbstractUpsiClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, UpsiConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
    }

    protected void setInitInput(int maxClientElementSize) {
        MathPreconditions.checkPositive("maxClientElementSize", maxClientElementSize);
        this.maxClientElementSize = maxClientElementSize;
        initState();
    }

    protected void setPtoInput(Set<T> clientElementSet) {
        checkInitialized();
        byte[] botElementByteArray = new byte[CommonConstants.STATS_BYTE_LENGTH];
        Arrays.fill(botElementByteArray, (byte)0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        MathPreconditions.checkPositiveInRangeClosed("clientElementSize", clientElementSet.size(), maxClientElementSize);
        clientElementList = clientElementSet.stream()
            .map(ObjectUtils::objectToByteArray)
            .map(ByteBuffer::wrap)
            .peek(yi -> Preconditions.checkArgument(!yi.equals(botElementByteBuffer), "xi must not equal ⊥"))
            .collect(Collectors.toCollection(ArrayList::new));
        clientElementSize = clientElementSet.size();
        byteArrayObjectMap = new HashMap<>(clientElementSize);
        clientElementSet.forEach(clientElement ->
            byteArrayObjectMap.put(ByteBuffer.wrap(ObjectUtils.objectToByteArray(clientElement)), clientElement)
        );
        extraInfo++;
    }
}