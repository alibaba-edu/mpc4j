package edu.alibaba.mpc4j.s2pc.pir.index.fastpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirParams;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * FastPIR协议客户端。
 *
 * @author Liqiang Peng
 * @date 2023/1/18
 */
public class Ayaa21IndexPirClient extends AbstractIndexPirClient {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * Fast PIR方案参数
     */
    private Ayaa21IndexPirParams params;
    /**
     * 公钥
     */
    private byte[] publicKey;
    /**
     * 私钥
     */
    private byte[] secretKey;
    /**
     * Galois密钥
     */
    private byte[] galoisKeys;

    public Ayaa21IndexPirClient(Rpc clientRpc, Party serverParty, Ayaa21IndexPirConfig config) {
        super(Ayaa21IndexPirPtoDesc.getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(AbstractIndexPirParams indexPirParams, int serverElementSize, int elementByteLength) {
        setInitInput(serverElementSize, elementByteLength);
        info("{}{} Client Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        assert (indexPirParams instanceof Ayaa21IndexPirParams);
        params = (Ayaa21IndexPirParams) indexPirParams;

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
        // 客户端生成问询
        List<byte[]> clientQueryPayload = Ayaa21IndexPirNativeUtils.generateQuery(
            params.getEncryptionParams(), publicKey, secretKey, index, params.getQuerySize()
        );
        // 添加Galois密钥
        clientQueryPayload.add(galoisKeys);
        // 发送问询
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Ayaa21IndexPirPtoDesc.PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientQueryHeader, clientQueryPayload));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), genQueryTime);

        // 客户端接收回复
        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Ayaa21IndexPirPtoDesc.PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> serverResponsePayload = rpc.receive(serverResponseHeader).getPayload();

        stopWatch.start();
        // 客户端解密
        byte[] result = handleServerResponsePayload(serverResponsePayload);
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), responseTime);

        info("{}{} Client end", ptoEndLogPrefix, getPtoDesc().getPtoName());
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
        int binNum = params.getBinNum();
        MpcAbortPreconditions.checkArgument(response.size() == binNum);
        byte[] result = new byte[elementByteLength];
        int binMaxByteLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength() / Byte.SIZE;
        int lastBinByteLength = elementByteLength % binMaxByteLength == 0 ?
            binMaxByteLength : elementByteLength % binMaxByteLength;
        IntStream intStream = this.parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        intStream.forEach(binIndex -> {
            int byteLength = binIndex == binNum - 1 ? lastBinByteLength : binMaxByteLength;
            long[] coeffs = Ayaa21IndexPirNativeUtils.decodeResponse(
                params.getEncryptionParams(), secretKey, response.get(binIndex)
            );
            int rowCount = coeffs.length / 2;
            long[][] rotatedCoeffs = new long[2][rowCount];
            IntStream.range(0, rowCount).forEach(i -> {
                rotatedCoeffs[0][i] = coeffs[(index + i) % rowCount];
                rotatedCoeffs[1][i] = coeffs[rowCount + ((index + i) % rowCount)];
            });
            byte[] upperBytes = convertCoeffsToBytes(rotatedCoeffs[0], params.getPlainModulusBitLength());
            byte[] lowerBytes = convertCoeffsToBytes(rotatedCoeffs[1], params.getPlainModulusBitLength());
            System.arraycopy(upperBytes, 0, result, binIndex * binMaxByteLength, byteLength / 2);
            System.arraycopy(lowerBytes, 0, result, binIndex * binMaxByteLength + byteLength / 2, byteLength / 2);
        });
        return result;
    }

    /**
     * 客户端生成密钥对。
     */
    private void generateKeyPair() {
        ArrayList<Integer> steps = new ArrayList<>();
        for (int i = 1; i < params.getElementColumnLength()[0]; i <<= 1) {
            steps.add(-i);
        }
        List<byte[]> keyPair = Ayaa21IndexPirNativeUtils.keyGen(
            params.getEncryptionParams(), steps.stream().mapToInt(step -> step).toArray()
        );
        assert (keyPair.size() == 3);
        this.publicKey = keyPair.remove(0);
        this.secretKey = keyPair.remove(0);
        this.galoisKeys = keyPair.remove(0);
    }
}