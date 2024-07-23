package edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.lll24;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.AbstractSstReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.SpCdpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * LLL24-SST receiver.
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
public class Lll24SstReceiver extends AbstractSstReceiver {
    /**
     * BP-CDPPRF
     */
    private final BpCdpprfSender bpCdpprfSender;
    /**
     * COT sender output
     */
    private CotSenderOutput cotSenderOutput;

    public Lll24SstReceiver(Rpc receiverRpc, Party senderParty, Lll24SstConfig config) {
        super(Lll24SstPtoDesc.getInstance(), receiverRpc, senderParty, config);
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
    public SstReceiverOutput shareTranslate(int num, int byteLength) throws MpcAbortException {
        setPtoInput(num, byteLength);
        return shareTranslate();
    }

    @Override
    public SstReceiverOutput shareTranslate(int num, int byteLength, CotSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(num, byteLength, preSenderOutput);
        this.cotSenderOutput = preSenderOutput;
        return shareTranslate();
    }

    private SstReceiverOutput shareTranslate() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // the parties are going to run N executions of OPV protocol to generate N vectors v_1, ... , v_n
        int paddingLogNum = LongUtils.ceilLog2(num);
        int paddingNum = (1 << paddingLogNum);
        int prfRowNum = num > 2 ? num - 1 : num;
        BpCdpprfSenderOutput senderOutput = bpCdpprfSender.puncture(prfRowNum, paddingNum, cotSenderOutput);
        cotSenderOutput = null;
        stopWatch.stop();
        long opvTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, opvTime);

        stopWatch.start();
        Prg prg = PrgFactory.createInstance(envType, byteLength);
        // P_1 sets elements of a, b to be column- and row-wise sums of the matrix elements
        byte[][][] extendMatrix = new byte[num][][];
        if (num > 2) {
            IntStream intStream = parallel ? IntStream.range(0, num - 1).parallel() : IntStream.range(0, num - 1);
            intStream.forEach(i -> {
                SpCdpprfSenderOutput eachSenderOutput = senderOutput.get(i);
                byte[][] eachMatrix = eachSenderOutput.getV0Array();
                byte[][] eachExtendMatrix = new byte[num][];
                for (int j = 0; j < num; j++) {
                    eachExtendMatrix[j] = prg.extendToBytes(eachMatrix[j]);
                }
                extendMatrix[i] = eachExtendMatrix;
            });
            intStream = parallel ? IntStream.range(0, num).parallel() : IntStream.range(0, num);
            extendMatrix[num - 1] = intStream.mapToObj(i -> {
                byte[] xorColumn = Arrays.copyOf(senderOutput.getDelta(), senderOutput.getDelta().length);
                for (int j = 0; j < num - 1; j++) {
                    BytesUtils.xori(xorColumn, senderOutput.get(j).getV0(i));
                }
                return prg.extendToBytes(xorColumn);
            }).toArray(byte[][]::new);
        } else {
            IntStream.range(0, num).forEach(i -> {
                SpCdpprfSenderOutput eachSenderOutput = senderOutput.get(i);
                byte[][] eachMatrix = eachSenderOutput.getV0Array();
                byte[][] eachExtendMatrix = new byte[num][];
                for (int j = 0; j < num; j++) {
                    eachExtendMatrix[j] = prg.extendToBytes(eachMatrix[j]);
                }
                extendMatrix[i] = eachExtendMatrix;
            });
        }
        byte[][] as = new byte[num][];
        byte[][] bs = new byte[num][];
        IntStream intStream = parallel ? IntStream.range(0, num).parallel() : IntStream.range(0, num);
        intStream.forEach(i -> {
            as[i] = new byte[byteLength];
            bs[i] = new byte[byteLength];
            for (int j = 0; j < num; j++) {
                BytesUtils.xori(as[i], extendMatrix[j][i]);
                BytesUtils.xori(bs[i], extendMatrix[i][j]);
            }
        });
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return new SstReceiverOutput(as, bs);
    }
}
