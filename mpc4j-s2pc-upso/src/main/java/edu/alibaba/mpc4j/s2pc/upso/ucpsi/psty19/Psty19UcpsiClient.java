package edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.NoStashCuckooHashBin;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtParty;
import edu.alibaba.mpc4j.s2pc.upso.okvr.OkvrFactory;
import edu.alibaba.mpc4j.s2pc.upso.okvr.OkvrReceiver;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.AbstractUcpsiClient;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiClientOutput;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19.Psty19UcpsiPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * PSTY19 unbalanced circuit PSI client.
 *
 * @author Liqiang Peng
 * @date 2023/4/17
 */
public class Psty19UcpsiClient<T> extends AbstractUcpsiClient<T> {
    /**
     * OKVR receiver
     */
    private final OkvrReceiver okvrReceiver;
    /**
     * peqt receiver
     */
    private final PeqtParty peqtParty;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * hash num
     */
    private final int hashNum;
    /**
     * β
     */
    private int beta;
    /**
     * l_peqt
     */
    private int peqtL;
    /**
     * l_peqt in byte
     */
    private int peqtByteL;
    /**
     * l_opprf in byte
     */
    private int opprfByteL;
    /**
     * cuckoo hash bin
     */
    private NoStashCuckooHashBin<T> cuckooHashBin;

    public Psty19UcpsiClient(Rpc clientRpc, Party serverParty, Psty19UcpsiConfig config) {
        super(Psty19UcpsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        okvrReceiver = OkvrFactory.createReceiver(clientRpc, serverParty, config.getOkvrConfig());
        addSubPto(okvrReceiver);
        peqtParty = PeqtFactory.createReceiver(clientRpc, serverParty, config.getPeqtConfig());
        addSubPto(peqtParty);
        cuckooHashBinType = CuckooHashBinType.NO_STASH_PSZ18_3_HASH;
        hashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void init(int maxClientElementSize, int serverElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, serverElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // β = (1 + ε) * n_c
        beta = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxClientElementSize);
        // point_num = hash_num * n_s
        int pointNum = hashNum * serverElementSize;
        // l_peqt = σ + log_2(β) + log_2(point_num)
        peqtL = CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2(beta);
        peqtByteL = CommonUtils.getByteLength(peqtL);
        // l_opprf = σ + log_2(point_num)
        int opprfL = Math.max(CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2(pointNum), peqtL);
        opprfByteL = CommonUtils.getByteLength(opprfL);
        stopWatch.stop();
        long paramTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, paramTime);

        stopWatch.start();
        okvrReceiver.init(pointNum, opprfL, beta);
        stopWatch.stop();
        long opprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, opprfTime);

        stopWatch.start();
        peqtParty.init(peqtL, beta);
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, peqtTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public UcpsiClientOutput<T> psi(Set<T> clientElementSet) throws MpcAbortException {
        setPtoInput(clientElementSet);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // receive hash keys
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();

        stopWatch.start();
        handleCuckooHashKeyPayload(cuckooHashKeyPayload);
        stopWatch.stop();
        long binTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, binTime, "Receiver hash elements");

        stopWatch.start();
        // OKVR
        ByteBuffer[] keyArray = IntStream.range(0, beta)
            .mapToObj(index -> {
                HashBinEntry<T> item = cuckooHashBin.getHashBinEntry(index);
                byte[] itemBytes = cuckooHashBin.getHashBinEntry(index).getItemByteArray();
                byte[] concatItemBytes = ByteBuffer.allocate(itemBytes.length + Integer.BYTES)
                    .put(itemBytes)
                    .putInt(item.getHashIndex())
                    .array();
                // we need to create a new instance otherwise the set would only have size 1.
                return ByteBuffer.wrap(concatItemBytes);
            })
            .toArray(ByteBuffer[]::new);
        Set<ByteBuffer> keys = Arrays.stream(keyArray).collect(Collectors.toSet());
        Map<ByteBuffer, byte[]> keyValueMap = okvrReceiver.okvr(keys);
        stopWatch.stop();
        long bopprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, bopprfTime, "Receiver batch opprf");

        stopWatch.start();
        byte[][] targetArray = Arrays.stream(keyArray)
            .map(keyValueMap::get)
            .map(target -> {
                byte[] truncatedTarget = new byte[peqtByteL];
                System.arraycopy(target, opprfByteL - peqtByteL, truncatedTarget, 0, peqtByteL);
                BytesUtils.reduceByteArray(truncatedTarget, peqtL);
                return truncatedTarget;
            })
            .toArray(byte[][]::new);
        // private equality test
        SquareZ2Vector z1 = peqtParty.peqt(peqtL, targetArray);
        ArrayList<T> table = IntStream.range(0, beta)
            .mapToObj(batchIndex -> {
                HashBinEntry<T> item = cuckooHashBin.getHashBinEntry(batchIndex);
                if (item.getHashIndex() == HashBinEntry.DUMMY_ITEM_HASH_INDEX) {
                    return null;
                } else {
                    return item.getItem();
                }
            })
            .collect(Collectors.toCollection(ArrayList::new));
        cuckooHashBin = null;
        UcpsiClientOutput<T> clientOutput = new UcpsiClientOutput<>(table, z1);
        stopWatch.stop();
        long membershipTestTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, membershipTestTime, "Receiver PEQT");

        return clientOutput;
    }

    private void handleCuckooHashKeyPayload(List<byte[]> cuckooHashKeyPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(cuckooHashKeyPayload.size() == hashNum);
        byte[][] hashKeys = cuckooHashKeyPayload.toArray(new byte[0][]);
        cuckooHashBin = CuckooHashBinFactory.createNoStashCuckooHashBin(envType, cuckooHashBinType, clientElementSize, hashKeys);
        cuckooHashBin.insertItems(clientElementArrayList);
        cuckooHashBin.insertPaddingItems(secureRandom);
    }
}
