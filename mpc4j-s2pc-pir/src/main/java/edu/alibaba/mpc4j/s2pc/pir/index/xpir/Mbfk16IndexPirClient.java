package edu.alibaba.mpc4j.s2pc.pir.index.xpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.xpir.Mbfk16IndexPirPtoDesc.PtoStep;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * XPIR协议客户端。
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public class Mbfk16IndexPirClient extends AbstractIndexPirClient {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * XPIR方案参数
     */
    private Mbfk16IndexPirParams params;

    public Mbfk16IndexPirClient(Rpc clientRpc, Party serverParty, Mbfk16IndexPirConfig config) {
        super(Mbfk16IndexPirPtoDesc.getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(IndexPirParams indexPirParams, int serverElementSize, int elementByteLength) throws MpcAbortException {
        setInitInput(serverElementSize, elementByteLength);
        info("{}{} Client Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        assert (indexPirParams instanceof Mbfk16IndexPirParams);
        params = (Mbfk16IndexPirParams) indexPirParams;

        initialized = true;
        info("{}{} Client Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public byte[] pir(int index) throws MpcAbortException {
        setPtoInput(index);
        info("{}{} Client begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 客户端生成BFV算法公私钥对
        List<byte[]> keyPair = Mbfk16IndexPirNativeUtils.keyGen(params.getEncryptionParams());
        // 客户端生成并发送问询
        List<byte[]> clientQueryPayload = generateQuery(params.getEncryptionParams(), keyPair.get(0), keyPair.get(1));
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientQueryHeader, clientQueryPayload));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), genQueryTime);

        stopWatch.start();
        // 客户端接收并解密回复
        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> serverResponsePayload = rpc.receive(serverResponseHeader).getPayload();
        byte[] element = handleServerResponsePayload(keyPair.get(1), serverResponsePayload);
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), responseTime);

        info("{}{} Client end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return element;
    }

    /**
     * 返回查询密文。
     *
     * @param encryptionParams 加密方案参数。
     * @param publicKey        公钥。
     * @param secretKey        私钥。
     * @return 查询密文。
     */
    public ArrayList<byte[]> generateQuery(byte[] encryptionParams, byte[] publicKey, byte[] secretKey) {
        int[] dimensionSize = params.getDimensionsLength();
        // index of FV plaintext
        int indexOfPlaintext = index / params.getElementSizeOfPlaintext();
        // 计算每个维度的坐标
        long[] indices = computeIndices(indexOfPlaintext, dimensionSize);
        ArrayList<Integer> plainQuery = new ArrayList<>();
        int pt;
        for (int i = 0; i < indices.length; i++) {
            for (int j = 0; j < dimensionSize[i]; j++) {
                // 第indices.get(i)个待加密明文为1, 其余明文为0
                pt = j == indices[i] ? 1 : 0;
                plainQuery.add(pt);
            }
        }
        // 返回查询密文
        return Mbfk16IndexPirNativeUtils.generateQuery(
            encryptionParams, publicKey, secretKey, plainQuery.stream().mapToInt(integer -> integer).toArray()
        );
    }

    /**
     * 解码回复得到检索结果。
     *
     * @param secretKey 私钥。
     * @param response  回复。
     * @return 检索结果。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private byte[] handleServerResponsePayload(byte[] secretKey, List<byte[]> response) throws MpcAbortException {
        long[] decodedCoefficientArray = Mbfk16IndexPirNativeUtils.decryptReply(
            params.getEncryptionParams(), secretKey, (ArrayList<byte[]>) response, params.getDimension()
        );
        byte[] decodeByteArray = convertCoeffsToBytes(decodedCoefficientArray);
        byte[] elementBytes = new byte[elementByteLength];
        // offset in FV plaintext
        int offset = index % params.getElementSizeOfPlaintext();
        System.arraycopy(decodeByteArray, offset * elementByteLength, elementBytes, 0, elementByteLength);
        return elementBytes;
    }

    /**
     * 将long型数组转换为字节数组。
     *
     * @param longArray long型数组。
     * @return 字节数组。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private byte[] convertCoeffsToBytes(long[] longArray) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(longArray.length == params.getPolyModulusDegree());
        int logt = params.getPlainModulusBitLength();
        int longArrayLength = longArray.length;
        byte[] byteArray = new byte[longArrayLength * logt / Byte.SIZE];
        int room = Byte.SIZE;
        int j = 0;
        for (long l : longArray) {
            long src = l;
            int rest = logt;
            while (rest != 0 && j < byteArray.length) {
                int shift = Math.min(room, rest);
                byteArray[j] = (byte) (byteArray[j] << shift);
                byteArray[j] = (byte) (byteArray[j] | (src >> (logt - shift)));
                src = src << shift;
                room -= shift;
                rest -= shift;
                if (room == 0) {
                    j++;
                    room = Byte.SIZE;
                }
            }
        }
        return byteArray;
    }

    /**
     * 计算各维度的坐标。
     *
     * @param retrievalIndex 索引值。
     * @param dimensionSize  各维度的长度。
     * @return 各维度的坐标。
     */
    private long[] computeIndices(int retrievalIndex, int[] dimensionSize) {
        long product = Arrays.stream(dimensionSize).asLongStream().reduce(1, (a, b) -> a * b);
        long[] indices = new long[dimensionSize.length];
        for (int i = 0; i < dimensionSize.length; i++) {
            product /= dimensionSize[i];
            long ji = retrievalIndex / product;
            indices[i] = ji;
            retrievalIndex -= ji * product;
        }
        return indices;
    }
}
