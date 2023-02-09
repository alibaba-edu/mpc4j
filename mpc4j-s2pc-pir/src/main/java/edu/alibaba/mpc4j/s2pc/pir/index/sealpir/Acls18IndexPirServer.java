package edu.alibaba.mpc4j.s2pc.pir.index.sealpir;

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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * SEAL PIR协议服务端。
 *
 * @author Liqiang Peng
 * @date 2023/1/17
 */
public class Acls18IndexPirServer extends AbstractIndexPirServer {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * XPIR方案参数
     */
    private Acls18IndexPirParams params;
    /**
     * BFV明文（点值表示）
     */
    private List<ArrayList<byte[]>> encodedDatabase;

    public Acls18IndexPirServer(Rpc serverRpc, Party clientParty, Acls18IndexPirConfig config) {
        super(Acls18IndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(AbstractIndexPirParams indexPirParams, ArrayList<ByteBuffer> elementArrayList,
                     int elementByteLength) {
        assert (indexPirParams instanceof Acls18IndexPirParams);
        params = (Acls18IndexPirParams) indexPirParams;
        info("{}{} Server Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
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

        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Acls18IndexPirPtoDesc.PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        ArrayList<byte[]> clientQueryPayload = new ArrayList<>(rpc.receive(clientQueryHeader).getPayload());

        // 服务端接收并处理问询
        stopWatch.start();
        ArrayList<byte[]> serverResponsePayload = handleClientQueryPayload(clientQueryPayload);
        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Acls18IndexPirPtoDesc.PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverResponseHeader, serverResponsePayload));
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
        int expectSize2 = 0;
        int[] nvec = params.getDimensionsLength()[0];
        ArrayList<ArrayList<byte[]>> clientQuery = new ArrayList<>();
        int expectSize1 = Arrays.stream(nvec).map(k -> (int) Math.ceil((k + 0.0) / params.getPolyModulusDegree())).sum();
        if ((binNum > 1) && (params.getPlaintextSize()[0] != params.getPlaintextSize()[binNum - 1])) {
            nvec = params.getDimensionsLength()[binNum - 1];
            expectSize2 = Arrays.stream(nvec).map(k -> (int) Math.ceil((k + 0.0) / params.getPolyModulusDegree())).sum();
            for (int i = 0; i < binNum - 1; i++) {
                clientQuery.add(new ArrayList<>());
                for (int j = 0; j < expectSize1; j++) {
                    clientQuery.get(i).add(clientQueryPayload.get(j));
                }
            }
            clientQuery.add(new ArrayList<>());
            for (int j = 0; j < expectSize2; j++) {
                clientQuery.get(binNum - 1).add(clientQueryPayload.get(expectSize1 + j));
            }
        } else {
            for (int i = 0; i < binNum; i++) {
                clientQuery.add(new ArrayList<>());
                for (int j = 0; j < expectSize1; j++) {
                    clientQuery.get(i).add(clientQueryPayload.get(j));
                }
            }
        }
        MpcAbortPreconditions.checkArgument(totalSize == expectSize1 + expectSize2 + 1);
        byte[] galoisKeys = clientQueryPayload.get(expectSize1 + expectSize2);
        IntStream intStream = this.parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        return intStream
            .mapToObj(i -> Acls18IndexPirNativeUtils.generateReply(
                params.getEncryptionParams(), galoisKeys, clientQuery.get(i), encodedDatabase.get(i),
                params.getDimensionsLength()[i])
            )
            .flatMap(Collection::stream).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 返回数据库编码后的多项式。
     *
     * @param binIndex 分块索引。
     * @return 数据库编码后的多项式。
     */
    private ArrayList<byte[]> preprocessDatabase(int binIndex) {
        int byteLength = elementByteArray.get(binIndex)[0].length;
        byte[] combinedBytes = new byte[num * byteLength];
        IntStream.range(0, num).forEach(index -> {
            byte[] element = elementByteArray.get(binIndex)[index];
            System.arraycopy(element, 0, combinedBytes, index * byteLength, byteLength);
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
        return Acls18IndexPirNativeUtils.nttTransform(params.getEncryptionParams(), coeffsList);
    }
}
