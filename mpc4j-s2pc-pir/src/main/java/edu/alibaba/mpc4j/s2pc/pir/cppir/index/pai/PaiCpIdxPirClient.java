package edu.alibaba.mpc4j.s2pc.pir.cppir.index.pai;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.crypto.stream.StreamCipher;
import edu.alibaba.mpc4j.common.tool.crypto.stream.StreamCipherFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractCpIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.pai.PaiCpIdxPirPtoDesc.PtoStep;
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
 * Pai client-specific preprocessing index PIR client.
 *
 * @author Weiran Liu
 * @date 2023/9/24
 */
public class PaiCpIdxPirClient extends AbstractCpIdxPirClient {
    /**
     * stream cipher
     */
    private final StreamCipher streamCipher;
    /**
     * rows
     */
    private int rows;
    /**
     * columns
     */
    private int columns;
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

    public PaiCpIdxPirClient(Rpc clientRpc, Party serverParty, PaiCpIdxPirConfig config) {
        super(PaiCpIdxPirPtoDesc.getInstance(), clientRpc, serverParty, config);
        streamCipher = StreamCipherFactory.createInstance(envType);
    }

    @Override
    public void init(int n, int l, int maxBatchNum) throws MpcAbortException {
        setInitInput(n, l, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        rows = PaiCpIdxPirUtils.getRowNum(n);
        columns = PaiCpIdxPirUtils.getColumnNum(n);
        assert rows * columns >= n
            : "rows * columns must be greater than or equal to n (" + n + "): " + rows * columns;
        stopWatch.stop();
        long paramTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(
            PtoState.INIT_STEP, 0, 1, paramTime,
            String.format(
                "Client sets params: n = %d, Rows = %d, Columns = %d, n (pad) = %d", n, rows, columns, rows * columns
            )
        );

        // preprocessing
        preprocessing();

        logPhaseInfo(PtoState.INIT_END);
    }

    private void preprocessing() throws MpcAbortException {
        stopWatch.start();
        // init keys
        byte[] ik1 = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        medPrp = PrpFactory.createInstance(envType);
        medPrp.setKey(ik1);
        byte[] ik2 = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        finalPrp = PrpFactory.createInstance(envType);
        finalPrp.setKey(ik2);
        vk = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        localCacheEntries = new TIntObjectHashMap<>();
        stopWatch.stop();
        long allocateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, allocateTime, "Client init keys");

        stopWatch.start();
        // stream receiving rows
        for (int iRow = 0; iRow < rows; iRow++) {
            int iFinalRow = iRow;
            // receive row stream database
            List<byte[]> rowRequestPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_ROW_STREAM_DATABASE_REQUEST.ordinal());
            MpcAbortPreconditions.checkArgument(rowRequestPayload.size() == 1);
            byte[] rowDataByteArray = rowRequestPayload.get(0);
            MpcAbortPreconditions.checkArgument(rowDataByteArray.length == byteL * columns);
            // split rows
            ByteBuffer rowByteBuffer = ByteBuffer.wrap(rowDataByteArray);
            byte[][] rowValueArray = new byte[columns][byteL];
            for (int iColumn = 0; iColumn < columns; iColumn++) {
                rowByteBuffer.get(rowValueArray[iColumn]);
            }
            byte[][] medKeyArray = new byte[columns][];
            byte[][] medValueArray = new byte[columns][];
            IntStream iColumnIndexStream = parallel ? IntStream.range(0, columns).parallel() : IntStream.range(0, columns);
            iColumnIndexStream.forEach(iColumn -> {
                // med key
                int key = iFinalRow * columns + iColumn;
                byte[] keyBytes = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH)
                    .putInt(CommonConstants.BLOCK_BYTE_LENGTH - Integer.BYTES, key)
                    .array();
                medKeyArray[iColumn] = medPrp.prp(keyBytes);
                // med value
                byte[] iv = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
                medValueArray[iColumn] = streamCipher.ivEncrypt(vk, iv, rowValueArray[iColumn]);
            });
            // send shuffled response
            List<Integer> rowPiList = IntStream.range(0, columns).boxed().collect(Collectors.toList());
            Collections.shuffle(rowPiList, secureRandom);
            int[] rowPi = rowPiList.stream().mapToInt(i -> i).toArray();
            ByteBuffer medByteBuffer = ByteBuffer.allocate((CommonConstants.BLOCK_BYTE_LENGTH * 2 + byteL) * columns);
            for (int iColumn = 0; iColumn < columns; iColumn++) {
                medByteBuffer.put(medKeyArray[rowPi[iColumn]]);
                medByteBuffer.put(medValueArray[rowPi[iColumn]]);
            }
            List<byte[]> rowResponsePayload = Collections.singletonList(medByteBuffer.array());
            sendOtherPartyPayload(PtoStep.CLIENT_SEND_MED_STREAM_DATABASE_RESPONSE.ordinal(), rowResponsePayload);
        }
        stopWatch.stop();
        long rowTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, rowTime, "Client handles " + rows + " rows");

        stopWatch.start();
        for (int iColumn = 0; iColumn < columns; iColumn++) {
            // receive column stream database
            List<byte[]> columnRequestPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_COLUMN_STREAM_DATABASE_REQUEST.ordinal());
            MpcAbortPreconditions.checkArgument(columnRequestPayload.size() == 1);
            byte[] columnDataByteArray = columnRequestPayload.get(0);
            // each request contains encrypted key + random IV + encrypted value
            MpcAbortPreconditions.checkArgument(
                columnDataByteArray.length == (CommonConstants.BLOCK_BYTE_LENGTH * 2 + byteL) * rows
            );
            // split columns
            ByteBuffer columnByteBuffer = ByteBuffer.wrap(columnDataByteArray);
            byte[][] columnKeyArray = new byte[rows][CommonConstants.BLOCK_BYTE_LENGTH];
            byte[][] columnValueArray = new byte[rows][CommonConstants.BLOCK_BYTE_LENGTH + byteL];
            for (int iRow = 0; iRow < rows; iRow++) {
                columnByteBuffer.get(columnKeyArray[iRow]);
                columnByteBuffer.get(columnValueArray[iRow]);
            }
            byte[][] finalKeyArray = new byte[rows][];
            byte[][] finalValueArray = new byte[rows][];
            IntStream iRowIndexStream = parallel ? IntStream.range(0, rows).parallel() : IntStream.range(0, rows);
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
            List<Integer> columnPiList = IntStream.range(0, rows).boxed().collect(Collectors.toList());
            Collections.shuffle(columnPiList, secureRandom);
            int[] columnPi = columnPiList.stream().mapToInt(i -> i).toArray();
            ByteBuffer finalByteBuffer = ByteBuffer.allocate((CommonConstants.BLOCK_BYTE_LENGTH * 2 + byteL) * rows);
            for (int iRow = 0; iRow < rows; iRow++) {
                finalByteBuffer.put(finalKeyArray[columnPi[iRow]]);
                finalByteBuffer.put(finalValueArray[columnPi[iRow]]);
            }
            List<byte[]> finalResponsePayload = Collections.singletonList(finalByteBuffer.array());
            sendOtherPartyPayload(PtoStep.CLIENT_SEND_FINAL_STREAM_DATABASE_RESPONSE.ordinal(), finalResponsePayload);
        }
        stopWatch.stop();
        long streamTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, streamTime, "Client handles " + columns + " columns");
    }

    @Override
    public byte[][] pir(int[] xs) throws MpcAbortException {
        setPtoInput(xs);
        byte[][] entries = new byte[xs.length][];
        for (int i = 0; i < xs.length; i++) {
            int x = xs[i];
            if (localCacheEntries.containsKey(x)) {
                entries[i] = requestLocalQuery(x);
            } else {
                entries[i] = requestActualQuery(x);
            }
        }
        return entries;
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
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal(), new LinkedList<>());
        stopWatch.stop();
        long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, queryTime, "Client requests empty query");

        List<byte[]> queryResponsePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal());

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(queryResponsePayload.isEmpty());
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
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal(), queryRequestPayload);
        stopWatch.stop();
        long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, queryTime, "Client requests query");

        List<byte[]> queryResponsePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal());

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
