package edu.alibaba.mpc4j.s2pc.pir.index.xpir;

import com.google.common.collect.Lists;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.xpir.Mbfk16IndexPirPtoDesc.PtoStep;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

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
    /**
     * 公钥
     */
    private byte[] publicKey;
    /**
     * 私钥
     */
    private byte[] secretKey;

    public Mbfk16IndexPirClient(Rpc clientRpc, Party serverParty, Mbfk16IndexPirConfig config) {
        super(Mbfk16IndexPirPtoDesc.getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(AbstractIndexPirParams indexPirParams, int serverElementSize, int elementByteLength) {
        setInitInput(serverElementSize, elementByteLength);
        info("{}{} Client Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        assert (indexPirParams instanceof Mbfk16IndexPirParams);
        params = (Mbfk16IndexPirParams) indexPirParams;

        stopWatch.start();
        // 客户端生成密钥对
        generateKeyPair();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Client Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public byte[] pir(int index) throws MpcAbortException {
        setPtoInput(index);
        info("{}{} Client begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 客户端生成并发送问询
        List<byte[]> clientQueryPayload = generateQuery();
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
        byte[] element = handleServerResponsePayload(serverResponsePayload);
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
     * @return 查询密文。
     */
    public ArrayList<byte[]> generateQuery() {
        int bundleNum = params.getBinNum();
        // 前n-1个分块
        int[] nvec = params.getDimensionsLength()[0];
        int indexOfPlaintext = index / params.getElementSizeOfPlaintext()[0];
        // 计算每个维度的坐标
        int[] indices = computeIndices(indexOfPlaintext, nvec);
        IntStream.range(0, indices.length)
            .forEach(i -> info("Client: index {} / {} = {} / {}", i + 1, indices.length, indices[i], nvec[i]));
        ArrayList<byte[]> result = new ArrayList<>(
            Mbfk16IndexPirNativeUtils.generateQuery(params.getEncryptionParams(), publicKey, secretKey, indices, nvec)
        );
        if ((bundleNum > 1) && (params.getPlaintextSize()[0] != params.getPlaintextSize()[bundleNum - 1])) {
            // 最后一个分块
            int[] lastNvec = params.getDimensionsLength()[bundleNum - 1];
            int lastIndexOfPlaintext = index / params.getElementSizeOfPlaintext()[bundleNum - 1];
            // 计算每个维度的坐标
            int[] lastIndices = computeIndices(lastIndexOfPlaintext, lastNvec);
            IntStream.range(0, lastIndices.length).forEach(i -> info("Client: last bundle index {} / {} = {} / {}",
                i + 1, lastIndices.length, lastIndices[i], lastNvec[i]));
            // 返回查询密文
            result.addAll(
                Mbfk16IndexPirNativeUtils.generateQuery(
                    params.getEncryptionParams(), publicKey, secretKey, lastIndices, lastNvec
                )
            );
        }
        return result;
    }

    /**
     * 解码回复得到检索结果。
     *
     * @param response 回复。
     * @return 检索结果。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private byte[] handleServerResponsePayload(List<byte[]> response) throws MpcAbortException {
        byte[] elementBytes = new byte[elementByteLength];
        int expansionRatio = params.getExpansionRatio();
        int binNum = params.getBinNum();
        int dimension = params.getDimension();
        int binResponseSize = IntStream.range(0, dimension - 1).map(i -> expansionRatio).reduce(1, (a, b) -> a * b);
        MpcAbortPreconditions.checkArgument(response.size() == binResponseSize * binNum);
        int binMaxByteLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength() / Byte.SIZE;
        int lastBinByteLength = elementByteLength % binMaxByteLength == 0 ?
            binMaxByteLength : elementByteLength % binMaxByteLength;
        IntStream intStream = this.parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        intStream.forEach(binIndex -> {
            int byteLength = binIndex == binNum - 1 ? lastBinByteLength : binMaxByteLength;
            long[] coeffs = Mbfk16IndexPirNativeUtils.decryptReply(
                params.getEncryptionParams(),
                secretKey,
                Lists.newArrayList(response.subList(binIndex * binResponseSize, (binIndex + 1) * binResponseSize)),
                params.getDimension()
            );
            byte[] bytes = convertCoeffsToBytes(coeffs, params.getPlainModulusBitLength());
            int offset = this.index % params.getElementSizeOfPlaintext()[binIndex];
            System.arraycopy(bytes, offset * byteLength, elementBytes, binIndex * binMaxByteLength, byteLength);
        });
        return elementBytes;
    }

    /**
     * 客户端生成密钥对。
     */
    private void generateKeyPair() {
        List<byte[]> keyPair = Mbfk16IndexPirNativeUtils.keyGen(params.getEncryptionParams());
        assert (keyPair.size() == 2);
        this.publicKey = keyPair.remove(0);
        this.secretKey = keyPair.remove(0);
    }
}
