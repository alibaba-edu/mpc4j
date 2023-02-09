package edu.alibaba.mpc4j.s2pc.pir.index.onionpir;

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
 * OnionPIR协议客户端。
 *
 * @author Liqiang Peng
 * @date 2022/11/11
 */
public class Mcr21IndexPirClient extends AbstractIndexPirClient {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * OnionPIR方案参数
     */
    private Mcr21IndexPirParams params;
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
    /**
     * 私钥密文
     */
    private ArrayList<byte[]> encryptedSecretKey = new ArrayList<>();

    public Mcr21IndexPirClient(Rpc clientRpc, Party serverParty, Mcr21IndexPirConfig config) {
        super(Mcr21IndexPirPtoDesc.getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(AbstractIndexPirParams indexPirParams, int serverElementSize, int elementByteLength) {
        setInitInput(serverElementSize, elementByteLength);
        assert (indexPirParams instanceof Mcr21IndexPirParams);
        params = (Mcr21IndexPirParams) indexPirParams;
        info("{}{} Client Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 客户端生成密钥对
        generateKeyPair();
        // 客户端加密私钥
        this.encryptedSecretKey = Mcr21IndexPirNativeUtils.encryptSecretKey(
            params.getEncryptionParams(), publicKey, secretKey
        );
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
        // 添加公钥
        clientQueryPayload.add(publicKey);
        // 添加Galois密钥
        clientQueryPayload.add(galoisKeys);
        // 添加私钥密文
        clientQueryPayload.addAll(encryptedSecretKey);
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Mcr21IndexPirPtoDesc.PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientQueryHeader, clientQueryPayload));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), genQueryTime);

        // 客户端接收并解密回复
        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Mcr21IndexPirPtoDesc.PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> serverResponsePayload = rpc.receive(serverResponseHeader).getPayload();
        stopWatch.start();
        byte[] result = handleServerResponsePayload(serverResponsePayload);
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), responseTime);

        info("{}{} Client end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return result;
    }

    /**
     * 返回查询密文。
     *
     * @return 查询密文。
     */
    public ArrayList<byte[]> generateQuery() {
        int binNum = params.getBinNum();
        // 前n-1个分块
        int[] nvec = params.getDimensionsLength()[0];
        int indexOfPlaintext = index / params.getElementSizeOfPlaintext()[0];
        // 计算每个维度的坐标
        int[] indices = computeIndices(indexOfPlaintext, nvec);
        IntStream.range(0, indices.length)
            .forEach(i -> info("Client: index {} / {} = {} / {}", i + 1, indices.length, indices[i], nvec[i]));
        ArrayList<byte[]> result = new ArrayList<>(
            Mcr21IndexPirNativeUtils.generateQuery(params.getEncryptionParams(), publicKey, secretKey, indices, nvec)
        );
        if ((binNum > 1) && (params.getPlaintextSize()[0] != params.getPlaintextSize()[binNum - 1])) {
            // 最后一个分块
            int[] lastNvec = params.getDimensionsLength()[binNum - 1];
            int lastIndexOfPlaintext = index / params.getElementSizeOfPlaintext()[binNum - 1];
            // 计算每个维度的坐标
            int[] lastIndices = computeIndices(lastIndexOfPlaintext, lastNvec);
            IntStream.range(0, lastIndices.length)
                .forEach(i -> info("Client: last bin index {} / {} = {} / {}",
                    i + 1, lastIndices.length, lastIndices[i], lastNvec[i]));
            // 返回查询密文
            result.addAll(
                Mcr21IndexPirNativeUtils.generateQuery(
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
     */
    private byte[] handleServerResponsePayload(List<byte[]> response) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(response.size() == params.getBinNum());
        byte[] result = new byte[elementByteLength];
        int binMaxByteLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength() / Byte.SIZE;
        int lastBinByteLength = elementByteLength % binMaxByteLength == 0 ?
            binMaxByteLength : elementByteLength % binMaxByteLength;
        int binNum = params.getBinNum();
        IntStream intStream = this.parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        intStream.forEach(i -> {
            int byteLength = i == binNum - 1 ? lastBinByteLength : binMaxByteLength;
            long[] coeffs = Mcr21IndexPirNativeUtils.decryptReply(
                params.getEncryptionParams(), secretKey, response.get(i)
            );
            byte[] bytes = convertCoeffsToBytes(coeffs, params.getPlainModulusBitLength());
            int offset = this.index % params.getElementSizeOfPlaintext()[i];
            System.arraycopy(bytes, offset * byteLength, result, i * binMaxByteLength, byteLength);
        });
        return result;
    }

    /**
     * 客户端生成密钥对。
     */
    private void generateKeyPair() {
        ArrayList<byte[]> keyPair = Mcr21IndexPirNativeUtils.keyGen(params.getEncryptionParams());
        assert (keyPair.size() == 3);
        this.publicKey = keyPair.remove(0);
        this.secretKey = keyPair.remove(0);
        this.galoisKeys = keyPair.remove(0);
    }
}