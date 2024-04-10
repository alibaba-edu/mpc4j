package edu.alibaba.mpc4j.s2pc.pso.psi.other.prty19;

import edu.alibaba.mpc4j.common.rpc.*;
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
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.TwoChoiceHashBin;
import edu.alibaba.mpc4j.common.tool.polynomial.gf2e.Gf2ePoly;
import edu.alibaba.mpc4j.common.tool.polynomial.gf2e.Gf2ePolyFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.prty19.Prty19FastPsiPtoDesc.PtoStep;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * PRTY19-PSI (fast computation) server.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/17
 */
public class Prty19FastPsiServer<T> extends AbstractPsiServer<T> {
    /**
     * core COT receiver
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * two-choice Hash keys
     */
    private byte[][] twoChoiceHashKeys;
    /**
     * filter type
     */
    private final FilterType filterType;
    /**
     * bin num
     */
    private int binNum;
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
     * x || 0 (in GF(2^l))
     */
    private byte[][] x0s;
    /**
     * x || 1 (in GF(2^l))
     */
    private byte[][] x1s;
    /**
     * PEQT hash
     */
    private Hash peqtHash;

    public Prty19FastPsiServer(Rpc serverRpc, Party clientParty, Prty19FastPsiConfig config) {
        super(Prty19FastPsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(serverRpc, clientParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
        filterType = config.getFilterType();
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // init COT
        int maxL = Prty19PsiUtils.getFastL(maxServerElementSize);
        coreCotReceiver.init(maxL);
        // receive two-choice hash keys
        DataPacketHeader twoChoiceHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_TWO_CHOICE_HASH_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> keyPayload = rpc.receive(twoChoiceHashKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(keyPayload.size() == 2);
        twoChoiceHashKeys = keyPayload.toArray(new byte[0][]);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
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
        Prp[] qPrps = IntStream.range(0, l)
            .mapToObj(i -> PrpFactory.createInstance(envType))
            .toArray(Prp[]::new);
        boolean[] s = new boolean[l];
        IntStream.range(0, l).forEach(i -> s[i] = secureRandom.nextBoolean());
        // Δ = s_1 || ... || s_l
        byte[] delta = BinaryUtils.binaryToRoundByteArray(s);
        binNum = TwoChoiceHashBin.expectedBinNum(clientElementSize);
        int binSize = TwoChoiceHashBin.expectedMaxBinSize(clientElementSize);
        initServerElements();
        stopWatch.stop();
        long setupTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, setupTime, "Server setups parameters");

        stopWatch.start();
        // Alice and Bob invoke l instances of Random OT, Alice receives output q_1, ..., q_l
        CotReceiverOutput cotReceiverOutput = coreCotReceiver.receive(s);
        RotReceiverOutput rotReceiverOutput = new RotReceiverOutput(envType, CrhfType.MMO, cotReceiverOutput);
        // init PRFs
        IntStream.range(0, l).forEach(i -> qPrps[i].setKey(rotReceiverOutput.getRb(i)));
        stopWatch.stop();
        long rotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, rotTime, "Server runs " + l + " Random OT");

        stopWatch.start();
        // Alice computes Q_b(x) = F(q_1, x || b) || F(q_2, x || b) || ... || F(q_l, x || b)
        byte[][] q0s = new byte[serverElementSize][byteL];
        byte[][] q1s = new byte[serverElementSize][byteL];
        IntStream serverElementIntStream = IntStream.range(0, serverElementSize);
        serverElementIntStream = parallel ? serverElementIntStream.parallel() : serverElementIntStream;
        serverElementIntStream.forEach(index -> {
            for (int i = 0; i < l; i++) {
                boolean q0i = (qPrps[i].prp(x0s[index])[0] & 0x01) != 0;
                BinaryUtils.setBoolean(q0s[index], offsetL + i, q0i);
                boolean q1i = (qPrps[i].prp(x1s[index])[0] & 0x01) != 0;
                BinaryUtils.setBoolean(q1s[index], offsetL + i, q1i);
            }
        });
        stopWatch.stop();
        long qsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, qsTime, "Server computes Q1(x) and Q2(x)");

        DataPacketHeader polynomialsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.CLIENT_SEND_POLYNOMIALS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> polynomialsPayload = rpc.receive(polynomialsHeader).getPayload();

        stopWatch.start();
        int m = Gf2ePolyFactory.getCoefficientNum(envType, binSize);
        MpcAbortPreconditions.checkArgument(polynomialsPayload.size() == m * binNum);
        byte[][] flattenPolynomials = polynomialsPayload.toArray(new byte[0][]);
        byte[][][] polynomials = IntStream.range(0, binNum)
            .mapToObj(binIndex -> {
                byte[][] polynomial = new byte[m][];
                System.arraycopy(flattenPolynomials, binIndex * m, polynomial, 0, m);
                return polynomial;
            })
            .toArray(byte[][][]::new);
        // compute and send filter 0
        List<byte[]> serverPrf0Payload = generatePrfPayload(0, x0s, q0s, delta, polynomials);
        DataPacketHeader serverPrf0Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PRFS_0.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverPrf0Header, serverPrf0Payload));
        // compute and send filter 1
        List<byte[]> serverPrf1Payload = generatePrfPayload(1, x1s, q1s, delta, polynomials);
        DataPacketHeader serverPrf1Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PRFS_1.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverPrf1Header, serverPrf1Payload));
        x0s = null;
        x1s = null;
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, serverPrfTime, "Server computes PRF");

        logPhaseInfo(PtoState.PTO_END);
    }

    private void initServerElements() {
        // init server elements
        x0s = new byte[serverElementSize][];
        x1s = new byte[serverElementSize][];
        Prf elementPrf = PrfFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
        elementPrf.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        IntStream elementIndexIntStream = IntStream.range(0, serverElementSize);
        elementIndexIntStream = parallel ? elementIndexIntStream.parallel() : elementIndexIntStream;
        elementIndexIntStream.forEach(index -> {
            byte[] x = ObjectUtils.objectToByteArray(serverElementArrayList.get(index));
            byte[] x0 = new byte[x.length + 1];
            x0[0] = 0;
            System.arraycopy(x, 0, x0, 1, x.length);
            x0s[index] = elementPrf.getBytes(x0);
            byte[] x1 = new byte[x.length + 1];
            x1[0] = 1;
            System.arraycopy(x, 0, x1, 1, x.length);
            x1s[index] = elementPrf.getBytes(x1);
        });
    }

    private List<byte[]> generatePrfPayload(int hashIndex, byte[][] xs, byte[][] qs, byte[] delta, byte[][][] polynomials) {
        Prf binHash = PrfFactory.createInstance(envType, Integer.BYTES);
        binHash.setKey(twoChoiceHashKeys[hashIndex]);
        IntStream serverElementIntStream = IntStream.range(0, serverElementSize);
        serverElementIntStream = parallel ? serverElementIntStream.parallel() : serverElementIntStream;
        List<byte[]> serverElementPrfs = serverElementIntStream
            .mapToObj(index -> {
                byte[] elementByteArray = ObjectUtils.objectToByteArray(serverElementArrayList.get(index));
                int binIndex = binHash.getInteger(elementByteArray, binNum);
                byte[] x = xs[index];
                byte[] ex = new byte[byteL];
                // note that l is always greater than 128
                System.arraycopy(x, 0, ex, byteL - x.length, x.length);
                // P(x)
                byte[] prf = gf2ePoly.evaluate(polynomials[binIndex], ex);
                // s · P(x)
                BytesUtils.andi(prf, delta);
                // Q(x) ⊕ s · P(x)
                BytesUtils.xori(prf, qs[index]);
                return peqtHash.digestToBytes(prf);
            })
            .collect(Collectors.toList());
        Collections.shuffle(serverElementPrfs, secureRandom);
        // create filter
        Filter<byte[]> prfFilter = FilterFactory.load(envType, filterType, serverElementSize, secureRandom);
        serverElementPrfs.forEach(prfFilter::put);
        return prfFilter.save();
    }
}

