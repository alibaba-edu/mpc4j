package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np99;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.AbstractBaseNotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BaseNotSenderOutput;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * NP99-基础n选1-OT协议发送方。
 *
 * @author Hanwen Feng
 * @date 2022/07/22
 */
public class Np99BaseNotSender extends AbstractBaseNotSender {
    /**
     * 基础OT协议发送方
     */
    private final BaseOtSender baseOtSender;
    /**
     * 最大选择数的比特长度
     */
    private int maxChoiceBitLength;

    public Np99BaseNotSender(Rpc senderRpc, Party receiverParty, Np99BaseNotConfig config) {
        super(Np99BaseNotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        baseOtSender = BaseOtFactory.createSender(senderRpc, receiverParty, config.getBaseOtConfig());
        addSubPto(baseOtSender);
    }

    @Override
    public void init(int maxChoice) throws MpcAbortException {
        setInitInput(maxChoice);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        maxChoiceBitLength = LongUtils.ceilLog2(maxChoice);
        baseOtSender.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public BaseNotSenderOutput send(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        BaseOtSenderOutput baseOtSenderOutput = baseOtSender.send(num * maxChoiceBitLength);
        stopWatch.stop();
        long otTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, otTime);

        stopWatch.start();
        boolean[][] binaryChoices = IntStream.range(0, maxChoice)
            .mapToObj(choice -> BinaryUtils.byteArrayToBinary(IntUtils.intToByteArray(choice), maxChoiceBitLength))
            .toArray(boolean[][]::new);
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        byte[][][] rMatrix = indexIntStream
            .mapToObj(index -> {
                byte[][] rnArray = new byte[maxChoice][];
                for (int choice = 0; choice < maxChoice; choice++) {
                    ByteBuffer rbBuffer = ByteBuffer.allocate(maxChoiceBitLength * CommonConstants.BLOCK_BYTE_LENGTH);
                    for (int i = 0; i < maxChoiceBitLength; i++) {
                        rbBuffer = binaryChoices[choice][i]
                            ? rbBuffer.put(baseOtSenderOutput.getR1(index * maxChoiceBitLength + i))
                            : rbBuffer.put(baseOtSenderOutput.getR0(index * maxChoiceBitLength + i));
                    }
                    rnArray[choice] = kdf.deriveKey(rbBuffer.array());
                }
                return rnArray;
            })
            .toArray(byte[][][]::new);
        stopWatch.stop();
        long sTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, sTime);

        logPhaseInfo(PtoState.PTO_END);
        return new BaseNotSenderOutput(maxChoice, rMatrix);
    }

}
