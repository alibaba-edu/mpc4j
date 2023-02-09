package edu.alibaba.mpc4j.s2pc.pir.index.onionpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirServer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * OnionPIR协议服务端。
 *
 * @author Liqiang Peng
 * @date 2022/11/14
 */
public class Mcr21IndexPirServer extends AbstractIndexPirServer {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * OnionPIR方案参数
     */
    private Mcr21IndexPirParams params;
    /**
     * Decomposed BFV明文
     */
    private List<ArrayList<long[]>> encodedDatabase;

    public Mcr21IndexPirServer(Rpc serverRpc, Party clientParty, Mcr21IndexPirConfig config) {
        super(Mcr21IndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(AbstractIndexPirParams indexPirParams, ArrayList<ByteBuffer> elementArrayList,
                     int elementByteLength) {
        assert (indexPirParams instanceof Mcr21IndexPirParams);
        params = (Mcr21IndexPirParams) indexPirParams;
        info("{}{} Server Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 一个多项式可表示的字节长度
        int binMaxByteLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength() / Byte.SIZE;
        setInitInput(elementArrayList, elementByteLength, binMaxByteLength, getPtoType().name());
        // 服务端对数据库进行编码
        int binNum = params.getBinNum();
        IntStream intStream = this.parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        encodedDatabase = intStream.mapToObj(this::preprocessDatabase).collect(Collectors.toList());
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Server Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();
        info("{}{} Server begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        // 服务端接收问询
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Mcr21IndexPirPtoDesc.PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        ArrayList<byte[]> clientQueryPayload = new ArrayList<>(rpc.receive(clientQueryHeader).getPayload());

        // 服务端处理问询
        stopWatch.start();
        ArrayList<byte[]> serverResponse = handleClientQueryPayload(clientQueryPayload);
        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Mcr21IndexPirPtoDesc.PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverResponseHeader, serverResponse));
        stopWatch.stop();
        long genResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), genResponseTime);

        info("{}{} Server end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    /**
     * 服务端处理客户端查询信息。
     *
     * @param clientQueryPayload 客户端查询信息。
     * @return 检索结果密文。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private ArrayList<byte[]> handleClientQueryPayload(ArrayList<byte[]> clientQueryPayload) throws MpcAbortException {
        int totalSize = clientQueryPayload.size();
        int binNum = params.getBinNum();
        ArrayList<ArrayList<byte[]>> clientQuery = new ArrayList<>();
        int expectSize, querySize1, querySize2 = 0;
        if (params.getDimensionsLength()[0].length == 1) {
            querySize1 = 2;
        } else {
            querySize1 = 3;
        }
        if ((binNum > 1) && (params.getPlaintextSize()[0] != params.getPlaintextSize()[binNum - 1])) {
            if (params.getDimensionsLength()[binNum - 1].length == 1) {
                querySize2 = 2;
            } else {
                querySize2 = 3;
            }
            for (int i = 0; i < binNum - 1; i++) {
                clientQuery.add(new ArrayList<>());
                for (int j = 0; j < querySize1; j++) {
                    clientQuery.get(i).add(clientQueryPayload.get(j));
                }
            }
            clientQuery.add(new ArrayList<>());
            for (int j = 0; j < querySize2; j++) {
                clientQuery.get(binNum - 1).add(clientQueryPayload.get(querySize1 + j));
            }
        } else {
            for (int i = 0; i < binNum; i++) {
                clientQuery.add(new ArrayList<>());
                for (int j = 0; j < querySize1; j++) {
                    clientQuery.get(i).add(clientQueryPayload.get(j));
                }
            }
        }
        int querySize = querySize1 + querySize2;
        expectSize = querySize + 2 + params.getGswDecompSize() * 2;
        MpcAbortPreconditions.checkArgument(totalSize == expectSize);
        byte[] publicKey = clientQueryPayload.get(querySize);
        byte[] galoisKeys = clientQueryPayload.get(querySize + 1);
        ArrayList<byte[]> encryptedSecretKey = new ArrayList<>(
            clientQueryPayload.subList(querySize + 2, querySize + 2 + params.getGswDecompSize() * 2)
        );
        IntStream intStream = this.parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        return intStream
            .mapToObj(i ->
                Mcr21IndexPirNativeUtils.generateReply(
                    params.getEncryptionParams(),
                    publicKey,
                    galoisKeys,
                    encryptedSecretKey,
                    clientQuery.get(i),
                    encodedDatabase.get(i),
                    params.getDimensionsLength()[i]
                ))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 返回数据库编码后的多项式。
     *
     * @param binIndex 分块索引。
     * @return 数据库编码后的多项式。
     */
    private ArrayList<long[]> preprocessDatabase(int binIndex) {
        int byteLength = elementByteArray.get(binIndex)[0].length;
        byte[] combinedBytes = new byte[num * byteLength];
        IntStream.range(0, num).forEach(i -> {
            byte[] element = elementByteArray.get(binIndex)[i];
            System.arraycopy(element, 0, combinedBytes, i * byteLength, byteLength);
        });
        // number of FV plaintexts needed to create the d-dimensional matrix
        int prod = Arrays.stream(params.getDimensionsLength()[binIndex]).reduce(1, (a, b) -> a * b);
        assert (params.getPlaintextSize()[binIndex] <= prod);
        ArrayList<long[]> coeffsList = new ArrayList<>();
        // 每个多项式包含的字节长度
        int byteSizeOfPlaintext = params.getElementSizeOfPlaintext()[binIndex] * byteLength;
        // 数据库总字节长度
        int totalByteSize = num * byteLength;
        // 一个多项式中需要使用的系数个数
        int usedCoeffSize = params.getElementSizeOfPlaintext()[binIndex] *
            ((int) Math.ceil(Byte.SIZE * byteLength / (double) params.getPlainModulusBitLength()));
        // 系数个数不大于多项式阶数
        assert (usedCoeffSize <= params.getPolyModulusDegree())
            : "coefficient num must be less than or equal to polynomial degree";
        // 字节转换为多项式系数
        int offset = 0;
        for (int i = 0; i < params.getPlaintextSize()[binIndex]; i++) {
            int processByteSize;
            if (totalByteSize <= offset) {
                break;
            } else if (totalByteSize < offset + byteSizeOfPlaintext) {
                processByteSize = totalByteSize - offset;
            } else {
                processByteSize = byteSizeOfPlaintext;
            }
            assert (processByteSize % byteLength == 0);
            // Get the coefficients of the elements that will be packed in plaintext i
            long[] coeffs = convertBytesToCoeffs(params.getPlainModulusBitLength(), offset, processByteSize, combinedBytes);
            assert (coeffs.length <= usedCoeffSize);
            offset += processByteSize;
            long[] paddingCoeffsArray = new long[params.getPolyModulusDegree()];
            System.arraycopy(coeffs, 0, paddingCoeffsArray, 0, coeffs.length);
            // Pad the rest with 1s
            IntStream.range(coeffs.length, params.getPolyModulusDegree()).forEach(j -> paddingCoeffsArray[j] = 1L);
            coeffsList.add(paddingCoeffsArray);
        }
        // Add padding plaintext to make database a matrix
        int currentPlaintextSize = coeffsList.size();
        assert (currentPlaintextSize <= params.getPlaintextSize()[binIndex]);
        IntStream.range(0, (prod - currentPlaintextSize))
            .mapToObj(i -> IntStream.range(0, params.getPolyModulusDegree()).mapToLong(i1 -> 1L).toArray())
            .forEach(coeffsList::add);
        return Mcr21IndexPirNativeUtils.preprocessDatabase(params.getEncryptionParams(), coeffsList);
    }
}
