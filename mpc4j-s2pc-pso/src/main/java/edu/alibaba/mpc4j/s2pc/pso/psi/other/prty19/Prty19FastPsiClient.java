package edu.alibaba.mpc4j.s2pc.pso.psi.other.prty19;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.structure.filter.Filter;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.TwoChoiceHashBin;
import edu.alibaba.mpc4j.common.tool.polynomial.gf2e.Gf2ePoly;
import edu.alibaba.mpc4j.common.tool.polynomial.gf2e.Gf2ePolyFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.prty19.Prty19FastPsiPtoDesc.PtoStep;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * PRTY19-PSI (fast computation) client.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/17
 */
public class Prty19FastPsiClient<T> extends AbstractPsiClient<T> {
    /**
     * core COT sender
     */
    private final CoreCotSender coreCotSender;
    /**
     * two-choice Hash keys
     */
    private byte[][] twoChoiceHashKeys;
    /**
     * l
     */
    private int l;
    /**
     * l (in byte)
     */
    private int byteL;
    /**
     * polynomial operation in GF(2^l). Appendix D states: For our fast protocol, we operate on GF(2^l).
     */
    private Gf2ePoly gf2ePoly;
    /**
     * PEQT hash
     */
    private Hash peqtHash;

    public Prty19FastPsiClient(Rpc clientRpc, Party serverParty, Prty19FastPsiConfig config) {
        super(Prty19FastPsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        coreCotSender = CoreCotFactory.createSender(clientRpc, serverParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // init COT
        int maxL = Prty19PsiUtils.getFastL(maxServerElementSize);
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        coreCotSender.init(delta, maxL);
        // generate and send two-choice hash keys
        twoChoiceHashKeys = CommonUtils.generateRandomKeys(2, secureRandom);
        List<byte[]> twoChoiceHashKeyPayload = Arrays.stream(twoChoiceHashKeys).collect(Collectors.toList());
        DataPacketHeader twoChoiceHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_TWO_CHOICE_HASH_KEY.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(twoChoiceHashKeyHeader, twoChoiceHashKeyPayload));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Set<T> psi(Set<T> clientElementSet, int serverElementSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // init PEQT hash
        int peqtByteLength = PsiUtils.getSemiHonestPeqtByteLength(serverElementSize, clientElementSize);
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        // init Finite field l
        l = Prty19PsiUtils.getFastL(serverElementSize);
        gf2ePoly = Gf2ePolyFactory.createInstance(envType, l);
        byteL = gf2ePoly.getByteL();
        int offsetL = byteL * Byte.SIZE - l;
        Prp[] tPrps = IntStream.range(0, l)
            .mapToObj(i -> PrpFactory.createInstance(envType))
            .toArray(Prp[]::new);
        Prp[] uPrps = IntStream.range(0, l)
            .mapToObj(i -> PrpFactory.createInstance(envType))
            .toArray(Prp[]::new);
        int binNum = TwoChoiceHashBin.expectedBinNum(clientElementSize);
        int binSize = TwoChoiceHashBin.expectedMaxBinSize(clientElementSize);
        stopWatch.stop();
        long setupTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, setupTime, "Client setups parameters");

        stopWatch.start();
        // Alice and Bob invoke l instances of Random OT, Alice receives output q_1, ..., q_l
        CotSenderOutput cotSenderOutput = coreCotSender.send(l);
        RotSenderOutput rotSenderOutput = new RotSenderOutput(envType, CrhfType.MMO, cotSenderOutput);
        // init PRFs
        IntStream.range(0, l).forEach(i -> tPrps[i].setKey(rotSenderOutput.getR0(i)));
        IntStream.range(0, l).forEach(i -> uPrps[i].setKey(rotSenderOutput.getR1(i)));
        stopWatch.stop();
        long rotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, rotTime, "Client runs " + l + " Random OT");

        stopWatch.start();
        // Bob inserts elements into two-choice hash
        TwoChoiceHashBin<T> twoChoiceHashBin = new TwoChoiceHashBin<>(
            envType, clientElementSize, twoChoiceHashKeys[0], twoChoiceHashKeys[1]
        );
        twoChoiceHashBin.insertItems(clientElementArrayList);
        twoChoiceHashBin.insertPaddingItems(secureRandom);
        ArrayList<ArrayList<HashBinEntry<T>>> entryMatrix = IntStream.range(0, binNum)
            .mapToObj(binIndex -> new ArrayList<>(twoChoiceHashBin.getBin(binIndex)))
            .collect(Collectors.toCollection(ArrayList::new));
        // Bob computes a polynomial for each bin
        byte[][][] tys = new byte[binNum][binSize][];
        Prf elementPrf = PrfFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
        elementPrf.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        IntStream binIndexIntStream = IntStream.range(0, twoChoiceHashBin.binNum());
        binIndexIntStream = parallel ? binIndexIntStream.parallel() : binIndexIntStream;
        List<byte[]> polynomialsPayload = binIndexIntStream
            .mapToObj(binIndex -> {
                Collection<HashBinEntry<T>> hashBinEntries = twoChoiceHashBin.getBin(binIndex);
                tys[binIndex] = new byte[binSize][];
                byte[][] yArray = hashBinEntries.stream()
                    .map(hashBinEntry -> {
                        int b = hashBinEntry.getHashIndex();
                        byte[] y = hashBinEntry.getItemByteArray();
                        byte[] yb = new byte[y.length + 1];
                        yb[0] = (byte) b;
                        System.arraycopy(y, 0, yb, 1, y.length);
                        yb = elementPrf.getBytes(yb);
                        return yb;
                    })
                    .toArray(byte[][]::new);
                byte[][] rArray = IntStream.range(0, binSize)
                    .mapToObj(index -> {
                        byte[] yb = yArray[index];
                        // T(y) = F(t_1, y) || F(t_2, y) || ... || F(t_l, y)
                        tys[binIndex][index] = new byte[byteL];
                        for (int i = 0; i < l; i++) {
                            boolean ti = (tPrps[i].prp(yb)[0] & 0x01) != 0;
                            BinaryUtils.setBoolean(tys[binIndex][index], offsetL + i, ti);
                        }
                        // U(y) = F(u_1, y) || F(u_2, y) || ... || F(u_l, y)
                        byte[] uy = new byte[byteL];
                        for (int i = 0; i < l; i++) {
                            boolean ui = (uPrps[i].prp(yb)[0] & 0x01) != 0;
                            BinaryUtils.setBoolean(uy, offsetL + i, ui);
                        }
                        byte[] ry = BytesUtils.xor(tys[binIndex][index], uy);
                        tys[binIndex][index] = peqtHash.digestToBytes(tys[binIndex][index]);
                        return ry;
                    })
                    .toArray(byte[][]::new);
                byte[][] eyArray = Arrays.stream(yArray)
                    .map(y -> {
                        // note that l is always greater than 128
                        byte[] ey = new byte[byteL];
                        System.arraycopy(y, 0, ey, ey.length - y.length, y.length);
                        return ey;
                    })
                    .toArray(byte[][]::new);
                return gf2ePoly.interpolate(binSize, eyArray, rArray);
            })
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
        DataPacketHeader polynomialsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.CLIENT_SEND_POLYNOMIALS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(polynomialsHeader, polynomialsPayload));
        stopWatch.stop();
        long rsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, rsTime, "Client computes R(y)");

        DataPacketHeader serverPrf0Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PRFS_0.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverPrf0Payload = rpc.receive(serverPrf0Header).getPayload();
        DataPacketHeader serverPrf1Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PRFS_1.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverPrf1Payload = rpc.receive(serverPrf1Header).getPayload();

        stopWatch.start();
        Filter<byte[]> prf0Filter = FilterFactory.load(envType, serverPrf0Payload);
        Filter<byte[]> prf1Filter = FilterFactory.load(envType, serverPrf1Payload);
        Set<T> intersection = IntStream.range(0, binNum)
            .mapToObj(binIndex -> IntStream.range(0, binSize)
                .mapToObj(index -> {
                    HashBinEntry<T> hashBinEntry = entryMatrix.get(binIndex).get(index);
                    if (hashBinEntry.getHashIndex() == HashBinEntry.DUMMY_ITEM_HASH_INDEX) {
                        return null;
                    } else if (hashBinEntry.getHashIndex() == 0) {
                        return prf0Filter.mightContain(tys[binIndex][index]) ? hashBinEntry.getItem() : null;
                    } else {
                        return prf1Filter.mightContain(tys[binIndex][index]) ? hashBinEntry.getItem() : null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()))
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, serverPrfTime, "Client computes intersection");

        logPhaseInfo(PtoState.PTO_END);
        return intersection;
    }
}