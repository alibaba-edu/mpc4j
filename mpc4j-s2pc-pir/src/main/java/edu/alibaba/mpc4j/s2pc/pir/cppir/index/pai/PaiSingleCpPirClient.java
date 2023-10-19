package edu.alibaba.mpc4j.s2pc.pir.cppir.index.pai;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.crypto.stream.StreamCipher;
import edu.alibaba.mpc4j.common.tool.crypto.stream.StreamCipherFactory;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractSingleCpPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.pai.PaiSingleCpPirDesc.PtoStep;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Pai client-specific preprocessing PIR client.
 *
 * @author Weiran Liu
 * @date 2023/9/24
 */
public class PaiSingleCpPirClient extends AbstractSingleCpPirClient {
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
     * value encrypted key
     */
    private byte[] vk;
    /**
     * med PRP
     */
    private Prp medPrp;
    /**
     * final PRP
     */
    private Prp finalPrp;
    /**
     * local cache entries
     */
    private TIntObjectMap<byte[]> localCacheEntries;

    public PaiSingleCpPirClient(Rpc clientRpc, Party serverParty, PaiSingleCpPirConfig config) {
        super(PaiSingleCpPirDesc.getInstance(), clientRpc, serverParty, config);
        streamCipher = StreamCipherFactory.createInstance(envType);
    }

    @Override
    public void init(int n, int l) throws MpcAbortException {
        setInitInput(n, l);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        rowNum = PaiSingleCpPirUtils.getRowNum(n);
        columnNum = PaiSingleCpPirUtils.getColumnNum(n);
        assert rowNum * columnNum >= n
            : "RowNum * ColumnNum must be greater than or equal to n (" + n + "): " + rowNum * columnNum;
        stopWatch.stop();
        long paramTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(
            PtoState.INIT_STEP, 0, 1, paramTime,
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
        byte[] ik1 = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(ik1);
        medPrp = PrpFactory.createInstance(envType);
        medPrp.setKey(ik1);
        byte[] ik2 = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(ik2);
        finalPrp = PrpFactory.createInstance(envType);
        finalPrp.setKey(ik2);
        vk = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(vk);
        localCacheEntries = new TIntObjectHashMap<>();
        stopWatch.stop();
        long allocateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, allocateTime, "Client init keys");

        stopWatch.start();
        // stream receiving rows
        for (int iRow = 0; iRow < rowNum; iRow++) {
            int iFinalRow = iRow;
            DataPacketHeader rowRequestHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_ROW_STREAM_DATABASE_REQUEST.ordinal(), extraInfo,
                otherParty().getPartyId(), rpc.ownParty().getPartyId()
            );
            List<byte[]> rowRequestPayload = rpc.receive(rowRequestHeader).getPayload();

            MpcAbortPreconditions.checkArgument(rowRequestPayload.size() == 1);
            byte[] rowDataByteArray = rowRequestPayload.get(0);
            MpcAbortPreconditions.checkArgument(rowDataByteArray.length == byteL * columnNum);
            // split rows
            ByteBuffer rowByteBuffer = ByteBuffer.wrap(rowDataByteArray);
            byte[][] rowValueArray = new byte[columnNum][byteL];
            for (int iColumn = 0; iColumn < columnNum; iColumn++) {
                rowByteBuffer.get(rowValueArray[iColumn]);
            }
            byte[][] medKeyArray = new byte[columnNum][];
            byte[][] medValueArray = new byte[columnNum][];
            IntStream iColumnIndexStream = IntStream.range(0, columnNum);
            iColumnIndexStream = parallel ? iColumnIndexStream.parallel() : iColumnIndexStream;
            iColumnIndexStream.forEach(iColumn -> {
                // med key
                int key = iFinalRow * columnNum + iColumn;
                byte[] keyBytes = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH)
                    .putInt(CommonConstants.BLOCK_BYTE_LENGTH - Integer.BYTES, key)
                    .array();
                medKeyArray[iColumn] = medPrp.prp(keyBytes);
                // med value
                byte[] iv = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(iv);
                medValueArray[iColumn] = streamCipher.ivEncrypt(vk, iv, rowValueArray[iColumn]);
            });
            // send shuffled response
            List<Integer> rowPiList = IntStream.range(0, columnNum).boxed().collect(Collectors.toList());
            Collections.shuffle(rowPiList, secureRandom);
            int[] rowPi = rowPiList.stream().mapToInt(i -> i).toArray();
            ByteBuffer medByteBuffer = ByteBuffer.allocate((CommonConstants.BLOCK_BYTE_LENGTH * 2 + byteL) * columnNum);
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
                columnDataByteArray.length == (CommonConstants.BLOCK_BYTE_LENGTH * 2 + byteL) * rowNum
            );
            // split columns
            ByteBuffer columnByteBuffer = ByteBuffer.wrap(columnDataByteArray);
            byte[][] columnKeyArray = new byte[rowNum][CommonConstants.BLOCK_BYTE_LENGTH];
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
                finalKeyArray[iRow] = finalPrp.prp(columnKeyArray[iRow]);
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
            ByteBuffer finalByteBuffer = ByteBuffer.allocate((CommonConstants.BLOCK_BYTE_LENGTH * 2 + byteL) * rowNum);
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
    public byte[] pir(int x) throws MpcAbortException {
        setPtoInput(x);

        if (localCacheEntries.containsKey(x)) {
            return requestLocalQuery(x);
        } else {
            return requestActualQuery(x);
        }
    }

    private byte[] requestLocalQuery(int x) throws MpcAbortException {
        // when client asks a query with x in cache, we make a dummy query, otherwise we would also leak information.
        if (localCacheEntries.size() == n) {
            // if all indexes have been queried, request an empty query and return value in local cache
            requestEmptyQuery();
        } else {
            // query a random index that is not queried before
            if (localCacheEntries.size() * 2 <= n) {
                // when queried size is not that large, sample a random query
                boolean success = false;
                int dummyX = -1;
                while (!success) {
                    dummyX = secureRandom.nextInt(n);
                    success = !localCacheEntries.containsKey(dummyX);
                }
                requestActualQuery(dummyX);
            } else {
                // when queries size reaches n / 2, it means we have O(n) storages, sample from the remaining set
                TIntSet remainIndexSet = new TIntHashSet(n);
                remainIndexSet.addAll(IntStream.range(0, n).toArray());
                remainIndexSet.removeAll(localCacheEntries.keys());
                int[] remainIndexArray = remainIndexSet.toArray();
                assert remainIndexArray.length > 0;
                requestActualQuery(remainIndexArray[0]);
            }
        }
        return localCacheEntries.get(x);
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

    private byte[] requestActualQuery(int x) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // PRP x two times
        byte[] keyBytes = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH)
            .putInt(CommonConstants.BLOCK_BYTE_LENGTH - Integer.BYTES, x)
            .array();
        byte[] medKey = medPrp.prp(keyBytes);
        byte[] finalKey = finalPrp.prp(medKey);
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

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(queryResponsePayload.size() == 1);
        byte[] responseByteArray = queryResponsePayload.get(0);
        MpcAbortPreconditions.checkArgument(responseByteArray.length == CommonConstants.BLOCK_BYTE_LENGTH + byteL);
        // decrypt
        byte[] value = streamCipher.ivDecrypt(vk, responseByteArray);
        // add x to the local cache
        localCacheEntries.put(x, value);
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime, "Client handles response");

        logPhaseInfo(PtoState.PTO_END);
        return value;
    }
}
