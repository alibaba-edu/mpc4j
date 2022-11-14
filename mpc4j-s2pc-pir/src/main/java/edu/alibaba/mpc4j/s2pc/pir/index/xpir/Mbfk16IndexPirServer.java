package edu.alibaba.mpc4j.s2pc.pir.index.xpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.xpir.Mbfk16IndexPirPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * XPIR协议服务端。
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public class Mbfk16IndexPirServer extends AbstractIndexPirServer {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * XPIR方案参数
     */
    private Mbfk16IndexPirParams params;
    /**
     * BFV明文（点值表示）
     */
    private ArrayList<byte[]> bfvPlaintext;

    public Mbfk16IndexPirServer(Rpc serverRpc, Party clientParty, Mbfk16IndexPirConfig config) {
        super(Mbfk16IndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(IndexPirParams indexPirParams, ArrayList<ByteBuffer> elementArrayList, int elementByteLength)
        throws MpcAbortException {
        setInitInput(elementArrayList, elementByteLength);
        assert (indexPirParams instanceof Mbfk16IndexPirParams);
        params = (Mbfk16IndexPirParams) indexPirParams;
        info("{}{} Server Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 服务端对数据库进行编码
        bfvPlaintext = encodeDatabase();
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

        stopWatch.start();
        // 服务端接收并处理问询
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        stopWatch.stop();
        long receiveQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), receiveQueryTime);

        stopWatch.start();
        ArrayList<byte[]> clientQueryPayload = new ArrayList<>(rpc.receive(clientQueryHeader).getPayload());
        int querySize = Arrays.stream(params.getDimensionsLength()).sum();
        MpcAbortPreconditions.checkArgument(clientQueryPayload.size() == querySize);
        ArrayList<byte[]> serverResponsePayload = Mbfk16IndexPirNativeUtils.generateReply(
            params.getEncryptionParams(), clientQueryPayload, bfvPlaintext, params.getDimensionsLength()
        );
        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverResponseHeader, serverResponsePayload));
        stopWatch.stop();
        long genResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), genResponseTime);

        info("{}{} Server end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    /**
     * 返回数据库编码后的点值表示多项式。
     *
     * @return 数据库编码后的点值表示多项式。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private ArrayList<byte[]> encodeDatabase() throws MpcAbortException {
        int logt = params.getPlainModulusBitLength();
        int polyModulusDegree = params.getPolyModulusDegree();
        // number of FV plaintexts needed to represent all elements
        int plaintextSize = params.getPlaintextSize();
        // number of FV plaintexts needed to create the d-dimensional matrix
        int prod = Arrays.stream(params.getDimensionsLength()).reduce(1, (a, b) -> a * b);
        MpcAbortPreconditions.checkArgument(plaintextSize <= prod);
        ArrayList<long[]> coeffsList = new ArrayList<>();
        int elementSizeOfPlaintext = params.getElementSizeOfPlaintext();
        // 每个多项式包含的字节长度
        int byteSizeOfPlaintext = elementSizeOfPlaintext * elementByteLength;
        // 数据库总字节长度
        int totalByteSize = num * elementByteLength;
        // 一个多项式中需要使用的系数个数
        int usedCoeffSize = elementSizeOfPlaintext * ((int) Math.ceil(Byte.SIZE * elementByteLength / (double) logt));
        // 系数个数不大于多项式阶数
        MpcAbortPreconditions.checkArgument(
            usedCoeffSize <= polyModulusDegree,
            "coefficient num = %s must be less than or equal to polynomial degree = %s",
            usedCoeffSize, polyModulusDegree
        );
        // 字节转换为多项式系数
        int offset = 0;
        for (int i = 0; i < plaintextSize; i++) {
            long processByteSize;
            if (totalByteSize <= offset) {
                break;
            } else if (totalByteSize < offset + byteSizeOfPlaintext) {
                processByteSize = totalByteSize - offset;
            } else {
                processByteSize = byteSizeOfPlaintext;
            }
            MpcAbortPreconditions.checkArgument(processByteSize % elementByteLength == 0);
            // Get the coefficients of the elements that will be packed in plaintext i
            long[] coeffsArray = convertBytesToCoeffs(logt, offset, processByteSize);
            MpcAbortPreconditions.checkArgument(coeffsArray.length <= usedCoeffSize);
            offset += processByteSize;
            long[] paddingCoeffsArray = new long[polyModulusDegree];
            System.arraycopy(coeffsArray, 0, paddingCoeffsArray, 0, coeffsArray.length);
            // Pad the rest with 1s
            IntStream.range(coeffsArray.length, polyModulusDegree).forEach(j -> paddingCoeffsArray[j] = 1L);
            coeffsList.add(paddingCoeffsArray);
        }
        // Add padding plaintext to make database a matrix
        int currentPlaintextSize = coeffsList.size();
        MpcAbortPreconditions.checkArgument(currentPlaintextSize <= plaintextSize);
        IntStream.range(0, (prod - currentPlaintextSize))
            .mapToObj(i -> IntStream.range(0, polyModulusDegree).mapToLong(i1 -> 1L).toArray())
            .forEach(coeffsList::add);
        // 多项式NTT转换
        return Mbfk16IndexPirNativeUtils.nttTransform(params.getEncryptionParams(), coeffsList);
    }

    /**
     * 将字节数组转换为指定比特长度的long型数组。
     *
     * @param limit  long型数值的比特长度。
     * @param offset 移位。
     * @param size   字节数组长度。
     * @return long型数组。
     */
    private long[] convertBytesToCoeffs(int limit, int offset, double size) {
        // 需要使用的系数个数
        int longArraySize = (int) Math.ceil(Byte.SIZE * size / (double) limit);
        long[] longArray = new long[longArraySize];
        int room = limit;
        int flag = 0;
        for (int i = 0; i < size; i++) {
            int src = elementByteArray[i+offset];
            if (src < 0) {
                src &= 0xFF;
            }
            int rest = Byte.SIZE;
            while (rest != 0) {
                if (room == 0) {
                    flag++;
                    room = limit;
                }
                int shift = Math.min(room, rest);
                long temp = longArray[flag] << shift;
                longArray[flag] = temp | (src >> (Byte.SIZE - shift));
                int remain = (1 << (Byte.SIZE - shift)) - 1;
                src = (src & remain) << shift;
                room -= shift;
                rest -= shift;
            }
        }
        longArray[flag] = longArray[flag] << room;
        return longArray;
    }
}
