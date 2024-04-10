package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.pai;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteFullEcc;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.crypto.stream.StreamCipher;
import edu.alibaba.mpc4j.common.tool.crypto.stream.StreamCipherFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.AbstractSingleCpKsPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.pai.PaiSingleCpCksPirDesc.PtoStep;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Pai client-specific preprocessing CKSPIR client.
 *
 * @author Liqiang Peng
 * @date 2023/9/20
 */
public class PaiSingleCpCksPirClient<T> extends AbstractSingleCpKsPirClient<T> {
    /**
     * sq-OPRF receiver
     */
    private final SqOprfReceiver sqOprfReceiver;
    /**
     * byte full ecc
     */
    private final ByteFullEcc byteFullEcc;
    /**
     * stream cipher
     */
    private final StreamCipher streamCipher;
    /**
     * row num
     */
    private int rowNum;
    /**
     * column num
     */
    private int columnNum;
    /**
     * ecc key α = α_1 α_2
     */
    private BigInteger alpha;
    /**
     * value encrypted key
     */
    private byte[] vk;
    /**
     * PRG used for encrypt concat value
     */
    private Prg prg;
    /**
     * local exist cache entries
     */
    private Map<T, byte[]> localExistCacheEntries;
    /**
     * local bot cache entries
     */
    private Set<T> localBotCacheEntries;

    public PaiSingleCpCksPirClient(Rpc clientRpc, Party serverParty, PaiSingleCpCksPirConfig config) {
        super(PaiSingleCpCksPirDesc.getInstance(), clientRpc, serverParty, config);
        sqOprfReceiver = SqOprfFactory.createReceiver(clientRpc, serverParty, config.getSqOprfConfig());
        addSubPto(sqOprfReceiver);
        byteFullEcc = ByteEccFactory.createFullInstance(envType);
        streamCipher = StreamCipherFactory.createInstance(envType);
    }

    @Override
    public void init(int n, int l) throws MpcAbortException {
        setInitInput(n, l);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        prg = PrgFactory.createInstance(envType, byteL);
        sqOprfReceiver.init(1);
        stopWatch.stop();
        long sqOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 5, sqOprfTime, "Client inits sq-OPRF");

        stopWatch.start();
        rowNum = PaiSingleCpCksPirUtils.getRowNum(n);
        columnNum = PaiSingleCpCksPirUtils.getColumnNum(n);
        assert rowNum * columnNum >= n
            : "RowNum * ColumnNum must be greater than or equal to n (" + n + "): " + rowNum * columnNum;
        stopWatch.stop();
        long paramTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(
            PtoState.INIT_STEP, 2, 5, paramTime,
            String.format(
                "Client sets params: n = %d, RowNum = %d, ColumnNum = %d, n (pad) = %d",
                n, rowNum, columnNum, rowNum * columnNum
            )
        );

        // preprocessing
        preprocessing();

        logPhaseInfo(PtoState.INIT_END);
    }

    private void preprocessing() throws MpcAbortException {
        stopWatch.start();
        // init keys
        BigInteger alpha1 = byteFullEcc.randomZn(secureRandom);
        BigInteger alpha2 = byteFullEcc.randomZn(secureRandom);
        alpha = alpha1.multiply(alpha2).mod(byteFullEcc.getN());
        vk = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(vk);
        localExistCacheEntries = new HashMap<>(n);
        localBotCacheEntries = new HashSet<>();
        stopWatch.stop();
        long allocateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 5, allocateTime, "Client init keys");

        stopWatch.start();
        // stream receiving rows
        for (int iRow = 0; iRow < rowNum; iRow++) {
            DataPacketHeader rowRequestHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_ROW_STREAM_DATABASE_REQUEST.ordinal(), extraInfo,
                otherParty().getPartyId(), rpc.ownParty().getPartyId()
            );
            List<byte[]> rowRequestPayload = rpc.receive(rowRequestHeader).getPayload();

            MpcAbortPreconditions.checkArgument(rowRequestPayload.size() == 1);
            byte[] rowDataByteArray = rowRequestPayload.get(0);
            MpcAbortPreconditions.checkArgument(rowDataByteArray.length == (byteFullEcc.pointByteLength() + byteL) * columnNum);
            // split rows
            ByteBuffer rowByteBuffer = ByteBuffer.wrap(rowDataByteArray);
            byte[][] rowKeyArray = new byte[columnNum][byteFullEcc.pointByteLength()];
            byte[][] rowValueArray = new byte[columnNum][byteL];
            for (int iColumn = 0; iColumn < columnNum; iColumn++) {
                rowByteBuffer.get(rowKeyArray[iColumn]);
                rowByteBuffer.get(rowValueArray[iColumn]);
            }
            byte[][] medKeyArray = new byte[columnNum][];
            byte[][] medValueArray = new byte[columnNum][];
            IntStream iColumnIndexStream = IntStream.range(0, columnNum);
            iColumnIndexStream = parallel ? iColumnIndexStream.parallel() : iColumnIndexStream;
            iColumnIndexStream.forEach(iColumn -> {
                // med key
                medKeyArray[iColumn] = byteFullEcc.mul(rowKeyArray[iColumn], alpha1);
                // med value
                byte[] iv = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(iv);
                medValueArray[iColumn] = streamCipher.ivEncrypt(vk, iv, rowValueArray[iColumn]);
            });
            // send shuffled response
            List<Integer> rowPiList = IntStream.range(0, columnNum).boxed().collect(Collectors.toList());
            Collections.shuffle(rowPiList, secureRandom);
            int[] rowPi = rowPiList.stream().mapToInt(i -> i).toArray();
            ByteBuffer medByteBuffer = ByteBuffer.allocate((byteFullEcc.pointByteLength() + CommonConstants.BLOCK_BYTE_LENGTH + byteL) * columnNum);
            for (int iColumn = 0; iColumn < columnNum; iColumn++) {
                medByteBuffer.put(medKeyArray[rowPi[iColumn]]);
                medByteBuffer.put(medValueArray[rowPi[iColumn]]);
            }
            List<byte[]> rowResponsePayload = Collections.singletonList(medByteBuffer.array());
            DataPacketHeader rowResponseHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_MED_STREAM_DATABASE_RESPONSE.ordinal(), extraInfo,
                rpc.ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(rowResponseHeader, rowResponsePayload));
            extraInfo++;
        }
        stopWatch.stop();
        long rowTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, rowTime, "Client handles " + rowNum + " rows");

        stopWatch.start();
        for (int iColumn = 0; iColumn < columnNum; iColumn++) {
            DataPacketHeader columnRequestHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_COLUMN_STREAM_DATABASE_REQUEST.ordinal(), extraInfo,
                otherParty().getPartyId(), rpc.ownParty().getPartyId()
            );
            List<byte[]> columnRequestPayload = rpc.receive(columnRequestHeader).getPayload();

            MpcAbortPreconditions.checkArgument(columnRequestPayload.size() == 1);
            byte[] columnDataByteArray = columnRequestPayload.get(0);
            // each request contains encrypted key + random IV + encrypted value
            MpcAbortPreconditions.checkArgument(
                columnDataByteArray.length == (byteFullEcc.pointByteLength() + CommonConstants.BLOCK_BYTE_LENGTH + byteL) * rowNum
            );
            // split columns
            ByteBuffer columnByteBuffer = ByteBuffer.wrap(columnDataByteArray);
            byte[][] columnKeyArray = new byte[rowNum][byteFullEcc.pointByteLength()];
            byte[][] columnValueArray = new byte[rowNum][CommonConstants.BLOCK_BYTE_LENGTH + byteL];
            for (int iRow = 0; iRow < rowNum; iRow++) {
                columnByteBuffer.get(columnKeyArray[iRow]);
                columnByteBuffer.get(columnValueArray[iRow]);
            }
            byte[][] finalKeyArray = new byte[rowNum][];
            byte[][] finalValueArray = new byte[rowNum][];
            IntStream iRowIndexStream = IntStream.range(0, rowNum);
            iRowIndexStream = parallel ? iRowIndexStream.parallel() : iRowIndexStream;
            iRowIndexStream.forEach(iRow -> {
                // final key
                finalKeyArray[iRow] = byteFullEcc.mul(columnKeyArray[iRow], alpha2);
                // final value
                byte[] value = streamCipher.ivDecrypt(vk, columnValueArray[iRow]);
                byte[] iv = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(iv);
                finalValueArray[iRow] = streamCipher.ivEncrypt(vk, iv, value);
            });
            // send shuffled response
            List<Integer> columnPiList = IntStream.range(0, rowNum).boxed().collect(Collectors.toList());
            Collections.shuffle(columnPiList, secureRandom);
            int[] columnPi = columnPiList.stream().mapToInt(i -> i).toArray();
            ByteBuffer finalByteBuffer = ByteBuffer.allocate((byteFullEcc.pointByteLength() + CommonConstants.BLOCK_BYTE_LENGTH + byteL) * rowNum);
            for (int iRow = 0; iRow < rowNum; iRow++) {
                finalByteBuffer.put(finalKeyArray[columnPi[iRow]]);
                finalByteBuffer.put(finalValueArray[columnPi[iRow]]);
            }
            List<byte[]> finalResponsePayload = Collections.singletonList(finalByteBuffer.array());
            DataPacketHeader finalResponseHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_FINAL_STREAM_DATABASE_RESPONSE.ordinal(), extraInfo,
                rpc.ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(finalResponseHeader, finalResponsePayload));
            extraInfo++;
        }
        stopWatch.stop();
        long streamTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, streamTime, "Client handles " + columnNum + " columns");
    }


    @Override
    public byte[] pir(T item) throws MpcAbortException {
        setPtoInput(item);

        if (localExistCacheEntries.containsKey(item)) {
            return requestLocalQuery(item);
        } else if (localBotCacheEntries.contains(item)) {
            return requestBotQuery();
        } else {
            return requestActualQuery(item);
        }
    }

    private byte[] requestLocalQuery(T item) throws MpcAbortException {
        requestEmptyQuery();
        return localExistCacheEntries.get(item);
    }

    private byte[] requestBotQuery() throws MpcAbortException {
        requestEmptyQuery();
        return null;
    }

    private void requestEmptyQuery() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        DataPacketHeader queryRequestHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(queryRequestHeader, new LinkedList<>()));
        stopWatch.stop();
        long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, queryTime, "Client requests empty query");

        DataPacketHeader queryResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryResponsePayload = rpc.receive(queryResponseHeader).getPayload();

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(queryResponsePayload.size() == 0);
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime, "Client handles empty response");

        logPhaseInfo(PtoState.PTO_END);
    }

    private byte[] requestActualQuery(T x) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // blind x two times
        byte[] xBytes = ObjectUtils.objectToByteArray(x);
        byte[] initHash = byteFullEcc.hashToCurve(xBytes);
        byte[] finalKey = byteFullEcc.mul(initHash, alpha);
        List<byte[]> queryRequestPayload = Collections.singletonList(finalKey);
        DataPacketHeader queryRequestHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(queryRequestHeader, queryRequestPayload));
        stopWatch.stop();
        long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, queryTime, "Client requests query");

        DataPacketHeader queryResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryResponsePayload = rpc.receive(queryResponseHeader).getPayload();

        MpcAbortPreconditions.checkArgument(queryResponsePayload.size() == 0 || queryResponsePayload.size() == 1);
        if (queryResponsePayload.size() == 0) {
            // no result
            stopWatch.start();
            localBotCacheEntries.add(x);
            stopWatch.stop();
            long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime, "Client handles ⊥");

            logPhaseInfo(PtoState.PTO_END);
            return null;
        } else {
            stopWatch.start();
            // contain result
            byte[] responseByteArray = queryResponsePayload.get(0);
            MpcAbortPreconditions.checkArgument(responseByteArray.length == CommonConstants.BLOCK_BYTE_LENGTH + byteL);
            // decrypt
            byte[] value = streamCipher.ivDecrypt(vk, responseByteArray);
            // run sq-OPRF and decrypt
            SqOprfReceiverOutput sqOprfReceiverOutput = sqOprfReceiver.oprf(new byte[][] {xBytes});
            byte[] key = prg.extendToBytes(sqOprfReceiverOutput.getPrf(0));
            byte[] plaintext = BytesUtils.xor(value, key);
            // add x to the local cache
            localExistCacheEntries.put(x, plaintext);
            stopWatch.stop();
            long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime, "Client handles response");

            logPhaseInfo(PtoState.PTO_END);
            return plaintext;
        }
    }
}