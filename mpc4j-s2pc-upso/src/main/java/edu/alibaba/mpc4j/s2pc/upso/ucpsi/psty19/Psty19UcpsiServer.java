package edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtParty;
import edu.alibaba.mpc4j.s2pc.upso.okvr.OkvrFactory;
import edu.alibaba.mpc4j.s2pc.upso.okvr.OkvrSender;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.AbstractUcpsiServer;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19.Psty19UcpsiPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * PSTY19 unbalanced circuit PSI server.
 *
 * @author Liqiang Peng
 * @date 2023/4/17
 */
public class Psty19UcpsiServer<T> extends AbstractUcpsiServer<T> {
    /**
     * OKVR sender
     */
    private final OkvrSender okvrSender;
    /**
     * peqt sender
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
     * hash keys
     */
    private byte[][] hashKeys;
    /**
     * target array
     */
    private byte[][] targetArray;
    /**
     * key-value map
     */
    private Map<ByteBuffer, byte[]> keyValueMap;

    public Psty19UcpsiServer(Rpc serverRpc, Party clientParty, Psty19UcpsiConfig config) {
        super(Psty19UcpsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        okvrSender = OkvrFactory.createSender(serverRpc, clientParty, config.getOkvrConfig());
        addSubPto(okvrSender);
        peqtParty = PeqtFactory.createSender(serverRpc, clientParty, config.getPeqtConfig());
        addSubPto(peqtParty);
        cuckooHashBinType = CuckooHashBinType.NO_STASH_PSZ18_3_HASH;
        hashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void init(Set<T> serverElementSet, int maxClientElementSize) throws MpcAbortException {
        setInitInput(serverElementSet, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // β = (1 + ε) * n_c
        beta = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxClientElementSize);
        // point_num = hash_num * n_s
        int pointNum = hashNum * serverElementSize;
        // l_peqt = σ + log_2(β)
        peqtL = CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2(beta);
        int peqtByteL = CommonUtils.getByteLength(peqtL);
        // l_opprf = σ + log_2(point_num)
        int opprfL = Math.max(CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2(pointNum), peqtL);
        int opprfByteL = CommonUtils.getByteLength(opprfL);
        // simple hash
        hashKeys = BlockUtils.randomBlocks(hashNum, secureRandom);
        // generate key-value map
        generateKeyValueMap(opprfL);
        stopWatch.stop();
        long binTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, binTime, "Sender hash elements");

        stopWatch.start();
        okvrSender.init(keyValueMap, opprfL, beta);
        stopWatch.stop();
        long opprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, opprfTime, "Sender init unbalanced batch opprf");

        stopWatch.start();
        // initialize peqt
        targetArray = Arrays.stream(targetArray)
            .map(target -> {
                byte[] truncatedTarget = new byte[peqtByteL];
                System.arraycopy(target, opprfByteL - peqtByteL, truncatedTarget, 0, peqtByteL);
                BytesUtils.reduceByteArray(truncatedTarget, peqtL);
                return truncatedTarget;
            })
            .toArray(byte[][]::new);
        peqtParty.init(peqtL, beta);
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, peqtTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector psi() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        // P1 sends hash keys
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HASH_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));

        stopWatch.start();
        okvrSender.okvr();
        stopWatch.stop();
        long senderBopprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, senderBopprfTime, "Sender execute unbalanced batch opprf");

        stopWatch.start();
        // private equality test
        SquareZ2Vector z2Vector = peqtParty.peqt(peqtL, targetArray);
        stopWatch.stop();
        long membershipTestTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, membershipTestTime, "Sender membership test");

        logPhaseInfo(PtoState.PTO_END);
        return z2Vector;
    }

    private void generateKeyValueMap(int l) {
        int byteL = CommonUtils.getByteLength(l);
        RandomPadHashBin<T> simpleHashBin = new RandomPadHashBin<>(envType, beta, serverElementSize, hashKeys);
        simpleHashBin.insertItems(serverElementArrayList);
        // P1 samples uniformly random and independent target values t_1, ..., t_β ∈ {0,1}^κ
        targetArray = IntStream.range(0, beta)
            .mapToObj(batchIndex -> BytesUtils.randomByteArray(byteL, l, secureRandom))
            .toArray(byte[][]::new);
        // P1 generates key-value pairs
        keyValueMap = new HashMap<>(beta * simpleHashBin.maxBinSize());
        IntStream.range(0, beta).forEach(index -> {
            // in each bin
            Set<HashBinEntry<T>> bin = simpleHashBin.getBin(index);
            for (HashBinEntry<T> entry : bin) {
                byte[] itemBytes = entry.getItemByteArray();
                // we cannot di
                byte[] bytesKey = ByteBuffer.allocate(itemBytes.length + Integer.BYTES)
                    .put(itemBytes)
                    .putInt(entry.getHashIndex())
                    .array();
                byte[] value = BytesUtils.clone(targetArray[index]);
                keyValueMap.put(ByteBuffer.wrap(bytesKey), value);
            }
        });
    }
}
