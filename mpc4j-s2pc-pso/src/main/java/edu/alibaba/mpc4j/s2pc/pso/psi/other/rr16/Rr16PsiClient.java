package edu.alibaba.mpc4j.s2pc.pso.psi.other.rr16;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.structure.filter.Filter;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory;
import edu.alibaba.mpc4j.common.structure.filter.SparseRandomBloomFilter;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * RR16 malicious PSI client.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/10/06
 */
public class Rr16PsiClient<T> extends AbstractPsiClient<T> {
    /**
     * core COT receiver
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * COT receiver output
     */
    private CotReceiverOutput cotReceiverOutput;
    /**
     * CT receiver
     */
    private final CoinTossParty ctReceiver;
    /**
     * Bloom Filter
     */
    private SparseRandomBloomFilter<byte[]> filter;
    /**
     * hash function for BF
     */
    private Prf gbfHash;
    /**
     * GBF Storage
     */
    private byte[][] gbfStorage;
    /**
     * OT choices
     */
    private boolean[] choiceBits;
    /**
     * OT number
     */
    private int nOt;
    /**
     * Set of challenge index
     */
    private TIntSet usedOne, usedZero;
    /**
     * Array of index of valid OT instances (0 & 1)
     */
    private Integer[] zeroIndex, oneIndex;
    /**
     * PEQT hash
     */
    private Hash peqtHash;


    public Rr16PsiClient(Rpc clientRpc, Party serverParty, Rr16PsiConfig config) {
        super(Rr16PsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(clientRpc, serverParty, config.getCoreCotConfig());
        ctReceiver = CoinTossFactory.createReceiver(clientRpc, serverParty, config.getCoinTossConfig());
        addSubPto(coreCotReceiver);
        addSubPto(ctReceiver);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        ctReceiver.init();
        byte[][] hashKeys = ctReceiver.coinToss(1, CommonConstants.BLOCK_BIT_LENGTH);
        int peqtByteLength = PsiUtils.getMaliciousPeqtByteLength(maxServerElementSize, maxClientElementSize);
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        nOt = Rr16PsiUtils.getOtBatchSize(maxClientElementSize);
        filter = SparseRandomBloomFilter.create(envType, maxClientElementSize, hashKeys[0]);

        gbfHash = PrfFactory.createInstance(envType, Integer.BYTES * SparseRandomBloomFilter.getHashNum(maxClientElementSize));
        gbfHash.setKey(hashKeys[0]);

        coreCotReceiver.init(nOt);
        stopWatch.stop();
        long initCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 4, initCotTime, "Client generates key");

        stopWatch.start();
        choiceBits = new boolean[nOt];
        Arrays.fill(choiceBits, 0, Rr16PsiUtils.getOtOneCount(maxClientElementSize), true);
        List<Boolean> choiceList = Arrays.asList(BinaryUtils.binaryToObjectBinary(choiceBits));
        Collections.shuffle(choiceList, secureRandom);
        choiceBits = BinaryUtils.objectBinaryToBinary(choiceList.toArray(new Boolean[nOt]));
        // run COT protocol
        cotReceiverOutput = coreCotReceiver.receive(choiceBits);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 4, cotTime, "Client OT");

        stopWatch.start();
        // generate and shuffle the index for 0/1 OT choice bit while waiting the challenge from server
        List<Integer> otZeroList = new LinkedList<>(), otOneList = new LinkedList<>();
        IntStream.range(0, nOt).forEach(index -> {
            if (choiceBits[index]) {
                otOneList.add(index);
            } else {
                otZeroList.add(index);
            }
        });
        Collections.shuffle(otZeroList, secureRandom);
        Collections.shuffle(otOneList, secureRandom);
        DataPacketHeader cncChallengeHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Rr16PsiPtoDesc.PtoStep.SERVER_SEND_CHANLLEGE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> cncChallengeList = rpc.receive(cncChallengeHeader).getPayload();
        stopWatch.stop();
        long challengeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 4, challengeTime, "Client receives challenge");

        stopWatch.start();
        List<byte[]> cncResponsePayload = genCncResponse(cncChallengeList);
        DataPacketHeader cncResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Rr16PsiPtoDesc.PtoStep.CLIENT_SEND_RESPONSE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(cncResponseHeader, cncResponsePayload));
        // generate the valid index array
        oneIndex = otOneList.stream().filter(s -> !usedOne.contains(s)).toArray(Integer[]::new);
        zeroIndex = otZeroList.stream().filter(s -> !usedOne.contains(s)).toArray(Integer[]::new);
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 4, 4, responseTime, "Client responses challenge");
        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Set<T> psi(Set<T> clientElementSet, int serverSetSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverSetSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        Stream<T> elementStream = parallel ? clientElementArrayList.stream().parallel() : clientElementArrayList.stream();
        byte[][] clientElementByteArrays = elementStream.map(ObjectUtils::objectToByteArray).toArray(byte[][]::new);
        IntStream.range(0, clientElementByteArrays.length).forEach(index -> filter.put(clientElementByteArrays[index]));
        // permutation
        List<byte[]> piPayload = generatePermutation();
        DataPacketHeader piHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Rr16PsiPtoDesc.PtoStep.CLIENT_SEND_PERMUTATION.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(piHeader, piPayload));
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, cotTime, "Client prepares inputs and generates Permutation");

        stopWatch.start();
        IntStream clientElementIndexIntStream = parallel ? IntStream.range(0, clientElementSize).parallel() : IntStream.range(0, clientElementSize);
        ArrayList<byte[]> clientOprfArrayList = clientElementIndexIntStream
            .mapToObj(index -> peqtHash.digestToBytes(Rr16PsiUtils.decode(gbfStorage, clientElementByteArrays[index], gbfHash)))
            .collect(Collectors.toCollection(ArrayList::new));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, oprfTime, "Client computes oprfs and hash outputs");

        stopWatch.start();
        DataPacketHeader serverPrfFilterHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Rr16PsiPtoDesc.PtoStep.SERVER_SEND_PRFS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverPrfFilterPayload = rpc.receive(serverPrfFilterHeader).getPayload();
        Filter<byte[]> serverPrfFilter = FilterFactory.load(envType, serverPrfFilterPayload);
        Set<T> intersection = IntStream.range(0, clientElementSize)
            .mapToObj(elementIndex -> {
                T element = clientElementArrayList.get(elementIndex);
                byte[] elementPrf = clientOprfArrayList.get(elementIndex);
                return serverPrfFilter.mightContain(elementPrf) ? element : null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        stopWatch.stop();
        long intersectionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, intersectionTime, "Client computes the intersection");
        logPhaseInfo(PtoState.PTO_END);
        return intersection;
    }

    List<byte[]> genCncResponse(List<byte[]> cncChallengeList) {
        usedOne = new TIntHashSet();
        usedZero = new TIntHashSet();
        assert cncChallengeList.size() <= nOt - filter.getM();
        List<byte[]> challenge = new LinkedList<>();
        byte[] response = new byte[cotReceiverOutput.getRb(0).length];
        cncChallengeList.forEach(x -> {
            int index = IntUtils.byteArrayToInt(x);
            if (cotReceiverOutput.getChoice(index)) {
                usedOne.add(index);
            } else {
                usedZero.add(index);
                BytesUtils.xori(response, cotReceiverOutput.getRb(index));
                challenge.add(x);
            }
        });
        challenge.add(response);
        return challenge;
    }

    private List<byte[]> generatePermutation() {
        byte[] filterBytes = filter.getStorage();
        gbfStorage = new byte[filter.getM()][];
        int[] indexes = new int[gbfStorage.length];
        for (int i = 0, start0 = 0, start1 = 0; i < gbfStorage.length; i++) {
            indexes[i] = BinaryUtils.getBoolean(filterBytes, i) || start0 >= zeroIndex.length ? oneIndex[start1++] : zeroIndex[start0++];
        }
        IntStream intStream = parallel ? IntStream.range(0, gbfStorage.length).parallel() : IntStream.range(0, gbfStorage.length);
        return intStream.mapToObj(index -> {
            gbfStorage[index] = cotReceiverOutput.getRb(indexes[index]);
            return IntUtils.intToByteArray(indexes[index]);
        }).collect(Collectors.toList());
    }
}