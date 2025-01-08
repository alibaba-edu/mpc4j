package edu.alibaba.mpc4j.s2pc.pso.psu.jsz22;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnPartyOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnSenderOutput;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pso.psu.AbstractOoPsuClient;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuClientOutput;
import edu.alibaba.mpc4j.s2pc.pso.psu.jsz22.Jsz22SfcPsuPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * JSZ22-SFC-PSU协议客户端。
 *
 * @author Weiran Liu
 * @date 2022/03/18
 */
public class Jsz22SfcPsuClient extends AbstractOoPsuClient {
    /**
     * OSN发送方
     */
    private final DosnSender dosnSender;
    /**
     * random-OSN
     */
    private final RosnSender rosnSender;
    /**
     * OPRF接收方
     */
    private final OprfReceiver oprfReceiver;
    /**
     * 核COT接收方
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * 布谷鸟哈希类型
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * 布谷鸟哈希函数数量
     */
    private final int cuckooHashNum;
    /**
     * cuckoo hash bin num
     */
    private int binNum;
    /**
     * 布谷鸟哈希
     */
    private CuckooHashBin<ByteBuffer> cuckooHashBin;
    /**
     * OPRF输出映射
     */
    private Hash oprfOutputMap;
    /**
     * 客户端OPRF集合
     */
    private Set<ByteBuffer> clientOprfSet;
    /**
     * save the precomputed rosn result
     */
    private RosnSenderOutput rosnSenderOutput;

    public Jsz22SfcPsuClient(Rpc clientRpc, Party serverParty, Jsz22SfcPsuConfig config) {
        super(Jsz22SfcPsuPtoDesc.getInstance(), clientRpc, serverParty, config);
        dosnSender = DosnFactory.createSender(clientRpc, serverParty, config.getOsnConfig());
        addSubPto(dosnSender);
        rosnSender = RosnFactory.createSender(clientRpc, serverParty, config.getRosnConfig());
        addSubPto(rosnSender);
        oprfReceiver = OprfFactory.createOprfReceiver(clientRpc, serverParty, config.getOprfConfig());
        addSubPto(oprfReceiver);
        coreCotReceiver = CoreCotFactory.createReceiver(clientRpc, serverParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
        cuckooHashBinType = config.getCuckooHashBinType();
        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxClientElementSize);
        // note that in PSU, we must use no-stash cuckoo hash
        int maxPrfNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType) * maxServerElementSize;
        dosnSender.init();
        rosnSender.init();
        oprfReceiver.init(maxBinNum, maxPrfNum);
        coreCotReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void preCompute(int clientElementSize, int serverElementSize, int elementByteLength) throws MpcAbortException {
        checkPrecomputeInput(clientElementSize, serverElementSize, elementByteLength);

        logPhaseInfo(PtoState.PTO_BEGIN, "Pre-computation");
        stopWatch.start();
        int precomputeBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, clientElementSize);
        rosnSenderOutput = rosnSender.rosn(precomputeBinNum, elementByteLength);
        stopWatch.stop();
        long precomputeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, precomputeTime);

        logPhaseInfo(PtoState.PTO_END, "Pre-computation");
    }

    @Override
    public PsuClientOutput psu(Set<ByteBuffer> clientElementSet, int serverElementSize, int elementByteLength)
        throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize, elementByteLength);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        binNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, clientElementSize);
        int oprfOutputByteLength = Jsz22SfcPsuPtoDesc.getOprfByteLength(binNum);
        oprfOutputMap = HashFactory.createInstance(getEnvType(), oprfOutputByteLength);
        // R inserts set Y into the Cuckoo hash table, and adds a dummy item d in each empty bin.
        List<byte[]> cuckooHashKeyPayload = generateCuckooHashKeyPayload();
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_CUCKOO_HASH_KEYS.ordinal(), cuckooHashKeyPayload);
        stopWatch.stop();
        long cuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 5, cuckooHashTime);

        stopWatch.start();
        // create client element vector (y_1, ..., y_m)
        byte[][] yVector = IntStream.range(0, binNum)
            .mapToObj(binIndex -> cuckooHashBin.getHashBinEntry(binIndex).getItemByteArray())
            .toArray(byte[][]::new);
        cuckooHashBin = null;
        // S and R invoke the ideal functionality F_{PS}.
        // R acts as P_0 with input set Y_C, obtains the shuffled shares {a_1, a_2, ... , a_b}.
        DosnPartyOutput osnSenderOutput;
        if (validPrecomputation()) {
            // if osn is pre-computed
            osnSenderOutput = dosnSender.dosn(yVector, elementByteLength, rosnSenderOutput);
            rosnSenderOutput = null;
        } else {
            osnSenderOutput = dosnSender.dosn(yVector, elementByteLength);
        }
        byte[][] aArray = IntStream.range(0, binNum)
            .mapToObj(osnSenderOutput::getShare)
            .toArray(byte[][]::new);
        stopWatch.stop();
        long osnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 5, osnTime);

        stopWatch.start();
        // S and R invoke the ideal functionality F_{mpOPRF}
        // R acts as P_0 with her shuffled shares {a_i}_{i ∈ [b]}, and obtains the outputs {F(k, a_i)}_{i ∈ [b]};
        OprfReceiverOutput oprfReceiverOutput = oprfReceiver.oprf(aArray);
        IntStream binIndexIntStream = parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        clientOprfSet = binIndexIntStream
            .mapToObj(oprfReceiverOutput::getPrf)
            .map(oprf -> oprfOutputMap.digestToBytes(oprf))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 5, oprfTime);

        stopWatch.start();
        // R checks if {F(k, a_j)}_{j ∈ [b]} ∩ I_i \neq ∅. If so, R sets b_i = 1, otherwise, sets bi = 0;
        List<byte[]> serverOprfPayload = receiveOtherPartyEqualSizePayload(
            PtoStep.SERVER_SEND_OPRFS.ordinal(), serverElementSize * cuckooHashNum, oprfOutputByteLength);
        boolean[] choices = handleServerOprfPayload(serverOprfPayload);
        int psica = (int) IntStream.range(0, choices.length).filter(i -> choices[i]).count();
        stopWatch.stop();
        long checkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 5, checkTime);

        stopWatch.start();
        CotReceiverOutput cotReceiverOutput = coreCotReceiver.receive(choices);
        List<byte[]> encPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_ENC_ELEMENTS.ordinal());
        MpcAbortPreconditions.checkArgument(encPayload.size() == serverElementSize);
        ArrayList<byte[]> encArrayList = new ArrayList<>(encPayload);
        // Y \cup Z
        Prg encPrg = PrgFactory.createInstance(envType, elementByteLength);
        IntStream decIntStream = parallel ? IntStream.range(0, serverElementSize).parallel() : IntStream.range(0, serverElementSize);
        Set<ByteBuffer> union = decIntStream
            .mapToObj(index -> {
                if (choices[index]) {
                    return botElementByteBuffer;
                } else {
                    // do not need CRHF since we call prg
                    byte[] message = encPrg.extendToBytes(cotReceiverOutput.getRb(index));
                    BytesUtils.xori(message, encArrayList.get(index));
                    return ByteBuffer.wrap(message);
                }
            })
            .collect(Collectors.toSet());
        union.addAll(clientElementSet);
        union.remove(botElementByteBuffer);
        stopWatch.stop();
        long unionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 5, unionTime);

        logPhaseInfo(PtoState.PTO_END);
        return new PsuClientOutput(union, psica);
    }

    private boolean validPrecomputation() {
        return rosnSenderOutput != null
            && rosnSenderOutput.getNum() == binNum
            && rosnSenderOutput.getByteLength() == elementByteLength;
    }

    private List<byte[]> generateCuckooHashKeyPayload() {
        cuckooHashBin = CuckooHashBinFactory.createEnforceNoStashCuckooHashBin(
            envType, cuckooHashBinType, clientElementSize, clientElementArrayList, secureRandom
        );
        cuckooHashBin.insertPaddingItems(botElementByteBuffer);
        return Arrays.stream(cuckooHashBin.getHashKeys()).collect(Collectors.toList());
    }

    private boolean[] handleServerOprfPayload(List<byte[]> serverOprfPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(serverOprfPayload.size() == serverElementSize * cuckooHashNum);
        byte[][] serverFlattenOprfs = serverOprfPayload.toArray(new byte[0][]);
        IntStream serverElementIndexStream = parallel ? IntStream.range(0, serverElementSize).parallel() : IntStream.range(0, serverElementSize);
        boolean[] choiceArray = new boolean[serverElementSize];
        serverElementIndexStream.forEach(index -> {
            for (int hashIndex = 0; hashIndex < cuckooHashNum; hashIndex++) {
                if (clientOprfSet.contains(ByteBuffer.wrap(serverFlattenOprfs[index * cuckooHashNum + hashIndex]))) {
                    choiceArray[index] = true;
                    break;
                }
            }
        });
        clientOprfSet = null;
        return choiceArray;
    }
}
