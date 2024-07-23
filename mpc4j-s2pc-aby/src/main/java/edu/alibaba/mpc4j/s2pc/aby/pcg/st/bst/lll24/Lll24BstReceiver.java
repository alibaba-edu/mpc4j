package edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.lll24;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstReceiverOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.AbstractBstReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.SpCdpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * LLL24-BST receiver.
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
public class Lll24BstReceiver extends AbstractBstReceiver {
    /**
     * BP-CDPPRF
     */
    private final BpCdpprfSender bpCdpprfSender;
    /**
     * COT sender output
     */
    private CotSenderOutput cotSenderOutput;

    public Lll24BstReceiver(Rpc receiverRpc, Party senderParty, Lll24BstConfig config) {
        super(Lll24BstPtoDesc.getInstance(), receiverRpc, senderParty, config);
        bpCdpprfSender = BpCdpprfFactory.createSender(receiverRpc, senderParty, config.getBpCdpprfConfig());
        addSubPto(bpCdpprfSender);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        bpCdpprfSender.init(delta);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public BstReceiverOutput shareTranslate(int batchNum, int eachNum, int byteLength) throws MpcAbortException {
        setPtoInput(batchNum, eachNum, byteLength);
        return shareTranslate();
    }

    @Override
    public BstReceiverOutput shareTranslate(int batchNum, int eachNum, int byteLength, CotSenderOutput preSenderOutput)
        throws MpcAbortException {
        setPtoInput(batchNum, eachNum, byteLength, preSenderOutput);
        this.cotSenderOutput = preSenderOutput;
        return shareTranslate();
    }

    private BstReceiverOutput shareTranslate() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // the parties are going to run N executions of OPV protocol to generate N vectors v_1, ... , v_n
        int paddingLogEachNum = LongUtils.ceilLog2(eachNum);
        int paddingEachNum = (1 << paddingLogEachNum);
        int realNum = eachNum > 2 ? eachNum - 1 : eachNum;
        BpCdpprfSenderOutput senderOutput = bpCdpprfSender.puncture(
            batchNum * realNum, paddingEachNum, cotSenderOutput
        );
        cotSenderOutput = null;
        stopWatch.stop();
        long opvTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, opvTime);

        stopWatch.start();
        Prg prg = PrgFactory.createInstance(envType, byteLength);
        IntStream batchIndexIntStream = IntStream.range(0, batchNum);
        batchIndexIntStream = parallel ? batchIndexIntStream.parallel() : batchIndexIntStream;
        SstReceiverOutput[] receiverOutputs = batchIndexIntStream
            .mapToObj(batchIndex -> {
                int offset = batchIndex * realNum;
                // P_1 sets elements of a, b to be column- and row-wise sums of the matrix elements
                byte[][][] extendMatrix = new byte[eachNum][][];
                if (eachNum > 2) {
                    IntStream.range(0, eachNum - 1).map(i -> offset + i).forEach(i -> {
                        SpCdpprfSenderOutput eachSenderOutput = senderOutput.get(i);
                        byte[][] eachMatrix = eachSenderOutput.getV0Array();
                        byte[][] eachExtendMatrix = new byte[eachNum][];
                        for (int j = 0; j < eachNum; j++) {
                            eachExtendMatrix[j] = prg.extendToBytes(eachMatrix[j]);
                        }
                        extendMatrix[i - offset] = eachExtendMatrix;
                    });
                    extendMatrix[eachNum - 1] = IntStream.range(0, eachNum).mapToObj(i -> {
                        byte[] xorColumn = Arrays.copyOf(senderOutput.getDelta(), senderOutput.getDelta().length);
                        for (int j = offset; j < eachNum + offset - 1; j++) {
                            BytesUtils.xori(xorColumn, senderOutput.get(j).getV0(i));
                        }
                        return prg.extendToBytes(xorColumn);
                    }).toArray(byte[][]::new);
                } else {
                    IntStream.range(0, eachNum).map(i -> offset + i).forEach(i -> {
                        SpCdpprfSenderOutput eachSenderOutput = senderOutput.get(i);
                        byte[][] eachMatrix = eachSenderOutput.getV0Array();
                        byte[][] eachExtendMatrix = new byte[eachNum][];
                        for (int j = 0; j < eachNum; j++) {
                            eachExtendMatrix[j] = prg.extendToBytes(eachMatrix[j]);
                        }
                        extendMatrix[i - offset] = eachExtendMatrix;
                    });
                }

                // P_1 sets elements of a, b to be column- and row-wise sums of the matrix elements
                byte[][] as = new byte[eachNum][];
                byte[][] bs = new byte[eachNum][];
                IntStream intStream = parallel ? IntStream.range(0, eachNum).parallel() : IntStream.range(0, eachNum);
                intStream.forEach(i -> {
                    as[i] = new byte[byteLength];
                    bs[i] = new byte[byteLength];
                    for (int j = 0; j < eachNum; j++) {
                        BytesUtils.xori(as[i], extendMatrix[j][i]);
                        BytesUtils.xori(bs[i], extendMatrix[i][j]);
                    }
                });
                return new SstReceiverOutput(as, bs);
            })
            .toArray(SstReceiverOutput[]::new);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return new BstReceiverOutput(receiverOutputs);
    }
}
