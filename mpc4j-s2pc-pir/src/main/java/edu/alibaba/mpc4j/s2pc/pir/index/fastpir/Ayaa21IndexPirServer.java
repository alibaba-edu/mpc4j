package edu.alibaba.mpc4j.s2pc.pir.index.fastpir;

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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * FastPIR协议服务端。
 *
 * @author Liqiang Peng
 * @date 2023/1/18
 */
public class Ayaa21IndexPirServer extends AbstractIndexPirServer {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * Fast PIR方案参数
     */
    private Ayaa21IndexPirParams params;
    /**
     * BFV明文（点值表示）
     */
    private List<ArrayList<byte[]>> encodedDatabase;

    public Ayaa21IndexPirServer(Rpc serverRpc, Party clientParty, Ayaa21IndexPirConfig config) {
        super(Ayaa21IndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(AbstractIndexPirParams indexPirParams, ArrayList<ByteBuffer> elementArrayList,
                     int elementByteLength) {
        assert (indexPirParams instanceof Ayaa21IndexPirParams);
        params = (Ayaa21IndexPirParams) indexPirParams;
        info("{}{} Server Init begin", ptoBeginLogPrefix, getPtoType().name());

        stopWatch.start();
        int binMaxByteLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength() / Byte.SIZE;
        setInitInput(elementArrayList, elementByteLength, binMaxByteLength, getPtoDesc().getPtoName());
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
            taskId, getPtoDesc().getPtoId(), Ayaa21IndexPirPtoDesc.PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        ArrayList<byte[]> clientQueryPayload = new ArrayList<>(rpc.receive(clientQueryHeader).getPayload());

        stopWatch.start();
        // 服务端接处理问询
        ArrayList<byte[]> serverResponsePayload = handleClientQueryPayload(clientQueryPayload);
        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Ayaa21IndexPirPtoDesc.PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
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
     * 数据库预处理。
     *
     * @param binIndex 分桶索引。
     * @return 编码后的数据库明文。
     */
    private ArrayList<byte[]> preprocessDatabase(int binIndex) {
        int coeffCount = params.getPolyModulusDegree();
        long[][] encodedDatabase = new long[params.getDatabaseRowNum()[binIndex]][coeffCount];
        int rowSize = params.getPolyModulusDegree() / 2;
        for (int i = 0; i < num; i++) {
            int rowIndex = i / rowSize;
            int colIndex = i % rowSize;
            byte[] element = elementByteArray.get(binIndex)[i];
            int length = element.length / 2;
            byte[] upperBytes = new byte[length];
            System.arraycopy(element, 0, upperBytes, 0, length);
            byte[] lowerBytes = new byte[length];
            System.arraycopy(element, length, lowerBytes, 0, length);
            long[] upperCoeffs = convertBytesToCoeffs(params.getPlainModulusBitLength(), 0, length, upperBytes);
            long[] lowerCoeffs = convertBytesToCoeffs(params.getPlainModulusBitLength(), 0, length, lowerBytes);
            for (int j = 0; j < params.getElementColumnLength()[binIndex]; j++) {
                encodedDatabase[rowIndex][colIndex] = upperCoeffs[j];
                encodedDatabase[rowIndex][colIndex + rowSize] = lowerCoeffs[j];
                rowIndex += params.getQuerySize();
            }
        }
        return Ayaa21IndexPirNativeUtils.nttTransform(params.getEncryptionParams(), encodedDatabase);
    }

    /**
     * 服务端处理客户端查询信息。
     *
     * @param clientQueryPayload 客户端查询信息。
     * @return 检索结果密文。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private ArrayList<byte[]> handleClientQueryPayload(ArrayList<byte[]> clientQueryPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(clientQueryPayload.size() == params.getQuerySize() + 1);
        ArrayList<byte[]> clientQuery = IntStream.range(0, params.getQuerySize())
            .mapToObj(clientQueryPayload::get)
            .collect(Collectors.toCollection(ArrayList::new));
        byte[] galoisKeys = clientQueryPayload.get(params.getQuerySize());
        int binNum = params.getBinNum();
        IntStream intStream = this.parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        return intStream
            .mapToObj(i -> Ayaa21IndexPirNativeUtils.generateResponse(
                params.getEncryptionParams(),
                galoisKeys,
                clientQuery,
                encodedDatabase.get(i),
                params.getElementColumnLength()[i]))
            .collect(Collectors.toCollection(ArrayList::new));
    }
}
