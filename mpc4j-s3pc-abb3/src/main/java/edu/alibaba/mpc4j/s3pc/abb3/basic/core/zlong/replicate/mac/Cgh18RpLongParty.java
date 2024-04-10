package edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.mac;

import edu.alibaba.mpc4j.common.circuit.zlong.MpcLongVector;
import edu.alibaba.mpc4j.common.circuit.zlong.PlainLongVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PartyState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.aby3.Aby3LongCpPtoDesc;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.FileUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.PrpUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.AbstractTripletLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.TripletLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.mac.Cgh18RpLongCpPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProvider;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongMacVector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

/**
 * The party of replicated zl64 sharing using MAC
 *
 * @author Feng Han
 * @date 2024/01/09
 */
public class Cgh18RpLongParty extends AbstractTripletLongParty implements TripletLongParty {
    /**
     * the directory to buffer the unverified and tuples
     */
    private final String bufferPath;
    /**
     * the maximum number of data in each buffer vectors
     */
    protected final int maxBufferElementLen;
    /**
     * the maximum number of memoryBuffer vectors
     */
    protected final int memoryBufferThreshold;
    /**
     * the maximum number of vectors can be verified at once
     */
    protected final int singleVerifyThreshold;
    /**
     * index for file buffer, the first one is the start index / the second one is the end index of unverified multiplication result in files
     */
    protected final int[] fileBufferIndexes;
    /**
     * storing unverified multiplication result
     */
    protected List<TripletRpLongMacVector> memoryBuffer;
    /**
     * valid number of data in the last memoryBuffer
     */
    protected int validNumOfLastBuffer;
    /**
     * current index of the mac key
     */
    protected int currentMacIndex;
    /**
     * current mac key
     */
    protected long[] shareMacKey;

    public Cgh18RpLongParty(Rpc rpc, Cgh18RpLongConfig config, TripletProvider tripletProvider) {
        super(Cgh18RpLongCpPtoDesc.getInstance(), rpc, config, tripletProvider);
        bufferPath = config.getBufferPath();
        File dir = new File(bufferPath);
        if (!dir.exists()) {
            boolean success = dir.mkdirs();
            if (!success) {
                throw new IllegalStateException("Dir " + dir.getName() + " doesn't exists and cannot create.");
            }
        }
        maxBufferElementLen = config.getMaxBufferElementLen();
        memoryBufferThreshold = config.getMemoryBufferThreshold();
        singleVerifyThreshold = config.getSingleVerifyThreshold();
        fileBufferIndexes = new int[]{0, 0};
        memoryBuffer = new LinkedList<>();
        currentMacIndex = 0;
    }

    @Override
    public void init() {
        if (partyState.equals(PartyState.INITIALIZED)) {
            return;
        }
        resetMac();
        initState();
    }

    public int getCurrentMacIndex() {
        return currentMacIndex;
    }

    /**
     * reset the mac key
     */
    private void resetMac() {
        long[][] rand = crProvider.getRandLongArray(1);
        currentMacIndex++;
        shareMacKey = new long[]{rand[0][0], rand[1][0]};
    }

    @Override
    public MpcLongVector create(boolean isPlain, LongVector... longVector) {
        if (isPlain) {
            assert longVector.length == 1;
            return PlainLongVector.create(longVector[0]);
        } else {
            assert longVector.length == 2;
            return TripletRpLongMacVector.create(longVector);
        }
    }

    @Override
    public MpcLongVector create(boolean isPlain, long[]... longs) {
        if (isPlain) {
            assert longs.length == 1;
            return PlainLongVector.create(longs[0]);
        } else {
            assert longs.length == 2;
            return TripletRpLongMacVector.create(longs);
        }
    }

    @Override
    public TripletRpLongMacVector createZeros(int dataNum) {
        return TripletRpLongMacVector.create(currentMacIndex,
            IntStream.range(0, 2).mapToObj(i -> LongVector.createZeros(dataNum)).toArray(LongVector[]::new),
            IntStream.range(0, 2).mapToObj(i -> LongVector.createZeros(dataNum)).toArray(LongVector[]::new));
    }

    @Override
    public TripletRpLongVector[] shareOwn(LongVector[] xiArray) throws MpcAbortException {
        // 1. generate a shared random vector, and reveal it to data owner
        int[] lens = Arrays.stream(xiArray).mapToInt(LongVector::getNum).toArray();
        TripletRpLongVector[] rShare = crProvider.randRpShareZl64Vector(lens);
        IntStream.range(0, rShare.length).forEach(i -> rShare[i] = TripletRpLongMacVector.create(rShare[i], false));
        duringVerificationFlag = true;
        LongVector[] r = revealOwn(64, rShare);
        duringVerificationFlag = false;
        // 2. compute w = v - r, and send it to two parties
        IntStream intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        LongVector[] w = intStream.mapToObj(i -> xiArray[i].sub(r[i])).toArray(LongVector[]::new);
        sendLongVectors(PtoStep.INPUT_SHARE.ordinal(), leftParty(), w);
        sendLongVectors(PtoStep.INPUT_SHARE.ordinal(), rightParty(), w);
        extraInfo++;
        // 3. send the received data to the other party, and compare
        compareView(w);
        // 4. get the result sharing  v = w + r
        IntStream.range(0, xiArray.length).forEach(i -> addi(rShare[i], PlainLongVector.create(w[i])));
        return rShare;
    }

    @Override
    public TripletRpLongVector[] shareOther(int[] nums, Party party) throws MpcAbortException {
        // 1. generate a shared random vector, and reveal it to data owner
        TripletRpLongVector[] rShare = crProvider.randRpShareZl64Vector(nums);
        IntStream.range(0, rShare.length).forEach(i -> rShare[i] = TripletRpLongMacVector.create(rShare[i], false));
        duringVerificationFlag = true;
        revealOther(party, rShare);
        duringVerificationFlag = false;
        // 2. compute w = v - r, and send it to two parties
        LongVector[] w = receiveLongVectors(PtoStep.INPUT_SHARE.ordinal(), party);
        extraInfo++;
        // 3. send the received data to the other party, and compare
        compareView(w);
        // 4. get the result sharing
        IntStream.range(0, nums.length).forEach(i -> addi(rShare[i], PlainLongVector.create(w[i])));
        return rShare;
    }

    @Override
    public TripletRpLongMacVector setPublicValue(LongVector xi) {
        LongVector[] macRes = Arrays.stream(shareMacKey).mapToObj(key ->
            LongVector.create(Arrays.stream(xi.getElements()).map(each -> each * key).toArray())
        ).toArray(LongVector[]::new);
        return TripletRpLongMacVector.create(currentMacIndex, new LongVector[]{
            selfId == 0 ? xi : LongVector.createZeros(xi.getNum()),
            selfId == 2 ? xi : LongVector.createZeros(xi.getNum())
        }, macRes);
    }

    @Override
    public LongVector[] revealOwn(int validBitLen, MpcLongVector... xiArray) throws MpcAbortException {
        if (xiArray == null) {
            return null;
        }
        checkUnverified();
        int dataDim = xiArray.length;
        LongVector[] d0 = Arrays.stream(xiArray).map(x -> x.getVectors()[0]).toArray(LongVector[]::new);
        LongVector[] d1 = Arrays.stream(xiArray).map(x -> x.getVectors()[1]).toArray(LongVector[]::new);
        // 1. receive data from left party and right party
        LongVector[] leftData = receiveLongVectors(PtoStep.REVEAL_SHARE.ordinal(), leftParty());
        LongVector[] rightData = receiveLongVectors(PtoStep.REVEAL_SHARE.ordinal(), rightParty());
        extraInfo++;
        // 2. assert equal
        assert Arrays.equals(Arrays.copyOfRange(leftData, 0, dataDim), Arrays.copyOfRange(rightData, dataDim, dataDim << 1));
        assert Arrays.equals(Arrays.copyOfRange(leftData, dataDim, dataDim << 1), d0);
        assert Arrays.equals(Arrays.copyOfRange(rightData, 0, dataDim), d1);
        // 3. reconstruct
        IntStream intStream = parallel ? IntStream.range(0, dataDim).parallel() : IntStream.range(0, dataDim);
        intStream.forEach(i -> {
            leftData[i].addi(d0[i]);
            leftData[i].addi(d1[i]);
            leftData[i].format(validBitLen);
        });
        return Arrays.copyOfRange(leftData, 0, dataDim);
    }

    @Override
    public void revealOther(Party party, MpcLongVector... xiArray) throws MpcAbortException {
        if (xiArray == null) {
            return;
        }
        checkUnverified();
        LongVector[] sendData = new LongVector[xiArray.length << 1];
        for (int i = 0; i < xiArray.length; i++) {
            sendData[i] = xiArray[i].getVectors()[0];
            sendData[i + xiArray.length] = xiArray[i].getVectors()[1];
        }
        sendLongVectors(PtoStep.REVEAL_SHARE.ordinal(), party, sendData);
        extraInfo++;
    }

    @Override
    public LongVector[] open(int validBits, MpcLongVector... xiArray) throws MpcAbortException {
        if (xiArray == null) {
            return null;
        }
        checkUnverified();
        LongVector[] sendData = Arrays.stream(xiArray).map(x -> x.getVectors()[0]).toArray(LongVector[]::new);
        sendLongVectors(Aby3LongCpPtoDesc.PtoStep.REVEAL_SHARE.ordinal(), rightParty(), sendData);

        LongVector[] data = receiveLongVectors(Aby3LongCpPtoDesc.PtoStep.REVEAL_SHARE.ordinal(), leftParty());
        IntStream intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        intStream.forEach(i -> {
            data[i].addi(xiArray[i].getVectors()[0]);
            data[i].addi(xiArray[i].getVectors()[1]);
            data[i].format(validBits);
        });

        extraInfo++;
        compareView(data);
        return data;
    }

    @Override
    public TripletLongVector add(MpcLongVector xi, MpcLongVector yi) {
        MpcLongVector[][] res = checkAndOrganizeInput(new MpcLongVector[]{xi}, new MpcLongVector[]{yi});
        TripletRpLongMacVector left = TripletRpLongMacVector.create((TripletRpLongVector) res[0][0], false);

        if (res[1][0].isPlain()) {
            MpcLongVector right = res[1][0];
            if (left.getMacIndex() == currentMacIndex) {
                TripletRpLongMacVector other = setPublicValue(right.getVectors()[0]);
                return TripletRpLongMacVector.create(currentMacIndex,
                    new LongVector[]{left.getVectors()[0].add(other.getVectors()[0]), left.getVectors()[1].add(other.getVectors()[1])},
                    new LongVector[]{left.getMacVec()[0].add(other.getMacVec()[0]), left.getMacVec()[1].add(other.getMacVec()[1])});
            } else {
                return (TripletRpLongMacVector) create(false,
                    selfId == 0 ? right.getVectors()[0].add(left.getVectors()[0]) : left.getVectors()[0].copy(),
                    selfId == 2 ? right.getVectors()[0].add(left.getVectors()[1]) : left.getVectors()[1].copy());
            }
        } else {
            if (res[1][0] instanceof TripletRpLongMacVector) {
                TripletRpLongMacVector right = (TripletRpLongMacVector) res[1][0];
                if (left.getMacIndex() == right.getMacIndex() && left.getMacIndex() == currentMacIndex) {
                    return TripletRpLongMacVector.create(currentMacIndex,
                        new LongVector[]{left.getVectors()[0].add(right.getVectors()[0]), left.getVectors()[1].add(right.getVectors()[1])},
                        new LongVector[]{left.getMacVec()[0].add(right.getMacVec()[0]), left.getMacVec()[1].add(right.getMacVec()[1])});
                }
            }
            return (TripletRpLongMacVector) create(false, res[1][0].getVectors()[0].add(left.getVectors()[0]), res[1][0].getVectors()[1].add(left.getVectors()[1]));
        }
    }

    @Override
    public void addi(MpcLongVector xi, MpcLongVector yi) {
        assert !xi.isPlain();
        if (xi instanceof TripletRpLongMacVector) {
            TripletRpLongMacVector left = (TripletRpLongMacVector) xi;
            if (yi.isPlain()) {
                if (left.getMacIndex() == currentMacIndex) {
                    TripletRpLongMacVector right = setPublicValue(yi.getVectors()[0]);
                    IntStream.range(0, 2).forEach(i -> {
                        left.getVectors()[i].addi(right.getVectors()[i]);
                        left.getMacVec()[i].addi(right.getMacVec()[i]);
                    });
                } else {
                    left.deleteMac();
                    if (selfId != 1) {
                        xi.getVectors()[selfId >> 1].addi(yi.getVectors()[0]);
                    }
                }
            } else {
                TripletRpLongMacVector right = TripletRpLongMacVector.create((TripletRpLongVector) yi, false);
                if (left.getMacIndex() == right.getMacIndex() && left.getMacIndex() == currentMacIndex) {
                    IntStream.range(0, 2).forEach(i -> {
                        left.getVectors()[i].addi(right.getVectors()[i]);
                        left.getMacVec()[i].addi(right.getMacVec()[i]);
                    });
                } else {
                    left.deleteMac();
                    IntStream.range(0, 2).forEach(i -> xi.getVectors()[i].addi(yi.getVectors()[i]));
                }
            }
        } else {
            if (yi.isPlain()) {
                if (selfId != 1) {
                    xi.getVectors()[selfId >> 1].addi(yi.getVectors()[0]);
                }
            } else {
                IntStream.range(0, 2).forEach(i -> xi.getVectors()[i].addi(yi.getVectors()[i]));
            }
        }
    }

    @Override
    public TripletLongVector sub(MpcLongVector xi, MpcLongVector yi) {
        assert !(xi.isPlain() && yi.isPlain());
        TripletRpLongMacVector left, right;
        if (xi.isPlain()) {
            if (yi instanceof TripletRpLongMacVector) {
                right = (TripletRpLongMacVector) yi;
                if (right.getMacIndex() == currentMacIndex) {
                    left = setPublicValue(xi.getVectors()[0]);
                    return subInner(left, right);
                }
            }
            return (TripletRpLongMacVector) create(false,
                selfId == 0 ? xi.getVectors()[0].sub(yi.getVectors()[0]) : yi.getVectors()[0].neg(),
                selfId == 2 ? xi.getVectors()[0].sub(yi.getVectors()[1]) : yi.getVectors()[1].neg());
        } else if (yi.isPlain()) {
            if (xi instanceof TripletRpLongMacVector) {
                left = (TripletRpLongMacVector) xi;
                if (left.getMacIndex() == currentMacIndex) {
                    right = setPublicValue(yi.getVectors()[0]);
                    return subInner(left, right);
                }
            }
            return (TripletRpLongMacVector) create(false,
                selfId == 0 ? xi.getVectors()[0].sub(yi.getVectors()[0]) : xi.getVectors()[0].copy(),
                selfId == 2 ? xi.getVectors()[1].sub(yi.getVectors()[0]) : xi.getVectors()[1].copy());
        } else {
            if (xi instanceof TripletRpLongMacVector && yi instanceof TripletRpLongMacVector) {
                left = (TripletRpLongMacVector) xi;
                right = (TripletRpLongMacVector) yi;
                if (left.getMacIndex() == currentMacIndex && right.getMacIndex() == currentMacIndex) {
                    return subInner(left, right);
                }
            }
            return TripletRpLongMacVector.create(IntStream.range(0, 2).mapToObj(i ->
                xi.getVectors()[i].sub(yi.getVectors()[i])).toArray(LongVector[]::new));
        }
    }

    private TripletRpLongMacVector subInner(TripletRpLongMacVector left, TripletRpLongMacVector right) {
        return TripletRpLongMacVector.create(currentMacIndex,
            new LongVector[]{left.getVectors()[0].sub(right.getVectors()[0]), left.getVectors()[1].sub(right.getVectors()[1])},
            new LongVector[]{left.getMacVec()[0].sub(right.getMacVec()[0]), left.getMacVec()[1].sub(right.getMacVec()[1])});
    }

    @Override
    public void subi(MpcLongVector xi, MpcLongVector yi) {
        assert !xi.isPlain();
        if (xi instanceof TripletRpLongMacVector) {
            TripletRpLongMacVector left = (TripletRpLongMacVector) xi;
            TripletRpLongMacVector right;
            if (yi.isPlain()) {
                if (left.getMacIndex() == currentMacIndex) {
                    right = setPublicValue(yi.getVectors()[0]);
                    IntStream.range(0, 2).forEach(i -> {
                        left.getVectors()[i].addi(right.getVectors()[i]);
                        left.getMacVec()[i].addi(right.getMacVec()[i]);
                    });
                } else {
                    left.deleteMac();
                    if (selfId != 1) {
                        xi.getVectors()[selfId >> 1].subi(yi.getVectors()[0]);
                    }
                }
            } else {
                right = TripletRpLongMacVector.create((TripletRpLongVector) yi, false);
                if (left.getMacIndex() != currentMacIndex || right.getMacIndex() != currentMacIndex) {
                    left.deleteMac();
                    IntStream.range(0, 2).forEach(i -> xi.getVectors()[i].subi(yi.getVectors()[i]));
                } else {
                    IntStream.range(0, 2).forEach(i -> {
                        left.getVectors()[i].addi(right.getVectors()[i]);
                        left.getMacVec()[i].addi(right.getMacVec()[i]);
                    });
                }
            }
        } else {
            if (yi.isPlain()) {
                if (selfId != 1) {
                    xi.getVectors()[selfId >> 1].subi(yi.getVectors()[0]);
                }
            } else {
                IntStream.range(0, 2).forEach(i -> xi.getVectors()[i].subi(yi.getVectors()[i]));
            }
        }

    }

    @Override
    public MpcLongVector neg(MpcLongVector xi) {
        if (xi.isPlain()) {
            return create(false, xi.getVectors()[0].neg());
        } else {
            TripletRpLongMacVector tmp = TripletRpLongMacVector.create((TripletRpLongVector) xi, false);
            if (tmp.getMacIndex() == currentMacIndex) {
                return TripletRpLongMacVector.create(currentMacIndex,
                    new LongVector[]{tmp.getVectors()[0].neg(), tmp.getVectors()[1].neg()},
                    new LongVector[]{tmp.getMacVec()[0].neg(), tmp.getMacVec()[1].neg()});
            } else {
                return create(false, Arrays.stream(xi.getVectors()).map(LongVector::neg).toArray(LongVector[]::new));
            }
        }
    }

    @Override
    public void negi(MpcLongVector xi) {
        if (xi.isPlain()) {
            xi.getVectors()[0].negi();
        } else {
            xi.getVectors()[0].negi();
            xi.getVectors()[1].negi();
            if (xi instanceof TripletRpLongMacVector) {
                TripletRpLongMacVector tmp = (TripletRpLongMacVector) xi;
                if (tmp.getMacIndex() == currentMacIndex) {
                    tmp.getMacVec()[0].negi();
                    tmp.getMacVec()[1].negi();
                } else {
                    tmp.deleteMac();
                }
            }
        }
    }

    /**
     * check and re-organize the inputs: we hope the left one should be: 1. private; 2. it is better to have mac
     */
    @Override
    protected MpcLongVector[][] checkAndOrganizeInput(MpcLongVector[] xiArray, MpcLongVector[] yiArray) {
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        for (int i = 0; i < xiArray.length; i++) {
            assert !(xiArray[i].isPlain() && yiArray[i].isPlain());
            assert xiArray[i].getNum() == yiArray[i].getNum();
        }

        int arrayLen = xiArray.length;
        TripletLongVector[] left = new TripletLongVector[arrayLen];
        MpcLongVector[] right = new MpcLongVector[arrayLen];

        for (int i = 0; i < arrayLen; i++) {
            if (xiArray[i].isPlain() || (yiArray[i] instanceof TripletRpLongMacVector && ((TripletRpLongMacVector) yiArray[i]).getMacIndex() != currentMacIndex)) {
                left[i] = (TripletLongVector) yiArray[i];
                right[i] = xiArray[i];
            } else {
                left[i] = (TripletLongVector) xiArray[i];
                right[i] = yiArray[i];
            }
        }
        return new MpcLongVector[][]{left, right};
    }

    @Override
    protected TripletRpLongMacVector[] mulPrivate(TripletLongVector[] xiArray, TripletLongVector[] yiArray) {
        genMac(xiArray);
        TripletRpLongMacVector[] left = Arrays.stream(xiArray).map(each -> (TripletRpLongMacVector) each).toArray(TripletRpLongMacVector[]::new);
        int[] nums = Arrays.stream(left).mapToInt(MpcLongVector::getNum).toArray();
        int totalNum = Arrays.stream(nums).sum();

        LongVector all0 = crProvider.randZeroZl64Vector(totalNum);
        LongVector all1 = crProvider.randZeroZl64Vector(totalNum);
        LongVector[] zeroShares = all0.split(nums);
        LongVector[] zeroMacShares = all1.split(nums);

        IntStream intStream = parallel ? IntStream.range(0, left.length).parallel() : IntStream.range(0, left.length);
        long[][] sendData = new long[left.length << 1][];
        intStream.forEach(i -> {
            zeroShares[i].addi(left[i].getVectors()[0].mul(yiArray[i].getVectors()[0]));
            zeroShares[i].addi(left[i].getVectors()[1].mul(yiArray[i].getVectors()[0]));
            zeroShares[i].addi(left[i].getVectors()[0].mul(yiArray[i].getVectors()[1]));
            sendData[i] = zeroShares[i].getElements();

            zeroMacShares[i].addi(left[i].getMacVec()[0].mul(yiArray[i].getVectors()[1]));
            zeroMacShares[i].addi(left[i].getMacVec()[1].mul(yiArray[i].getVectors()[0]));
            zeroMacShares[i].addi(left[i].getMacVec()[1].mul(yiArray[i].getVectors()[1]));
            sendData[i + left.length] = zeroMacShares[i].getElements();
        });
        sendLong(PtoStep.MUL_OP.ordinal(), leftParty(), sendData);

        long[][] fromRight = receiveLong(PtoStep.MUL_OP.ordinal(), rightParty());
        intStream = parallel ? IntStream.range(0, left.length).parallel() : IntStream.range(0, left.length);
        TripletRpLongMacVector[] res = intStream.mapToObj(i ->
            TripletRpLongMacVector.create(currentMacIndex,
                new LongVector[]{zeroShares[i], LongVector.create(fromRight[i])},
                new LongVector[]{zeroMacShares[i], LongVector.create(fromRight[i + xiArray.length])})
        ).toArray(TripletRpLongMacVector[]::new);
        intoBuffer(res);
        extraInfo++;
        return res;
    }

    @Override
    protected TripletRpLongMacVector mulPublic(TripletLongVector xiArray, PlainLongVector yiArray) {
        TripletRpLongMacVector left = (TripletRpLongMacVector) xiArray;
        LongVector[] innerNew = Arrays.stream(left.getVectors()).map(each -> each.mul(yiArray.getVectors()[0])).toArray(LongVector[]::new);
        if (left.getMacIndex() == currentMacIndex) {
            LongVector[] macNew = Arrays.stream(left.getMacVec()).map(each -> each.mul(yiArray.getVectors()[0])).toArray(LongVector[]::new);
            return TripletRpLongMacVector.create(currentMacIndex, innerNew, macNew);
        } else {
            return TripletRpLongMacVector.create(innerNew);
        }
    }

    @Override
    public void muli(MpcLongVector[] xiArray, PlainLongVector[] yiArray) {
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        for (int i = 0; i < xiArray.length; i++) {
            assert !(xiArray[i].isPlain() && yiArray[i].isPlain());
            assert xiArray[i].getNum() == yiArray[i].getNum();
        }
        IntStream intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        intStream.forEach(i -> {
            xiArray[i].getVectors()[0].muli(yiArray[i].getVectors()[0]);
            xiArray[i].getVectors()[1].muli(yiArray[i].getVectors()[0]);
            if (xiArray[i] instanceof TripletRpLongMacVector && ((TripletRpLongMacVector) xiArray[i]).getMacIndex() == currentMacIndex) {
                TripletRpLongMacVector tmp = (TripletRpLongMacVector) xiArray[i];
                if (tmp.getMacIndex() == currentMacIndex) {
                    tmp.getMacVec()[0].muli(yiArray[i].getVectors()[0]);
                    tmp.getMacVec()[1].muli(yiArray[i].getVectors()[0]);
                } else {
                    tmp.deleteMac();
                }
            }
        });
    }

    public MpcLongVector add(MpcLongVector xi, long constValue) {
        if (xi.isPlain()) {
            return PlainLongVector.create(Arrays.stream(xi.getVectors()[0].getElements()).map(each -> each + constValue).toArray());
        } else {
            LongVector[] innerVec = IntStream.range(0, 2).mapToObj(i ->
                selfId == i << 1
                    ? LongVector.create(Arrays.stream(xi.getVectors()[i].getElements()).map(each -> each + constValue).toArray())
                    : xi.getVectors()[i].copy()
            ).toArray(LongVector[]::new);
            if (xi instanceof TripletRpLongMacVector) {
                TripletRpLongMacVector that = (TripletRpLongMacVector) xi;
                if (that.getMacIndex() == currentMacIndex) {
                    long[] constMacValue = new long[]{constValue * shareMacKey[0], constValue * shareMacKey[1]};
                    LongVector[] macVec = IntStream.range(0, 2).mapToObj(i ->
                        LongVector.create(Arrays.stream(that.getMacVec()[i].getElements()).map(each -> each + constMacValue[i]).toArray())
                    ).toArray(LongVector[]::new);
                    return TripletRpLongMacVector.create(currentMacIndex, innerVec, macVec);
                }
            }
            return create(false, innerVec);
        }
    }

    @Override
    public void addi(MpcLongVector xi, long constValue) {
        if (xi.isPlain()) {
            long[] newArray = Arrays.stream(xi.getVectors()[0].getElements()).map(each -> each + constValue).toArray();
            xi.setVectors(LongVector.create(newArray));
        } else {
            if (selfId == 0) {
                long[] newArray = Arrays.stream(xi.getVectors()[0].getElements()).map(each -> each + constValue).toArray();
                xi.setVectors(LongVector.create(newArray), xi.getVectors()[1]);
            }
            if (selfId == 2) {
                long[] newArray = Arrays.stream(xi.getVectors()[1].getElements()).map(each -> each + constValue).toArray();
                xi.setVectors(xi.getVectors()[0], LongVector.create(newArray));
            }
            if (xi instanceof TripletRpLongMacVector) {
                TripletRpLongMacVector that = (TripletRpLongMacVector) xi;
                if (that.getMacIndex() == currentMacIndex) {
                    long[] constMacValue = new long[]{constValue * shareMacKey[0], constValue * shareMacKey[1]};
                    LongVector[] macVec = IntStream.range(0, 2).mapToObj(i ->
                        LongVector.create(Arrays.stream(that.getMacVec()[i].getElements()).map(each -> each + constMacValue[i]).toArray())
                    ).toArray(LongVector[]::new);
                    that.setMacVec(macVec);
                }
            }
        }
    }


    @Override
    public void verifyMul() throws MpcAbortException {
        int fileNum = fileBufferIndexes[1] - fileBufferIndexes[0];
        if ((memoryBuffer.isEmpty() || (memoryBuffer.size() == 1 && validNumOfLastBuffer == 0)) && fileNum == 0) {
            return;
        }
        if (validNumOfLastBuffer > 0) {
            memoryBuffer.get(memoryBuffer.size() - 1).reduce(validNumOfLastBuffer);
        } else {
            memoryBuffer.remove(memoryBuffer.size() - 1);
        }

        // 1. generate a random coin, and open the mac key
        long[][] shareR = crProvider.getRandLongArray(2);
        long[][] openData = new long[2][3];
        for (int i = 0; i < 2; i++) {
            openData[i][0] = shareR[i][0];
            openData[i][1] = shareR[i][1];
            openData[i][2] = shareMacKey[i];
        }
        long[] coinAndMacKey = open(create(false, openData[0], openData[1]))[0].getElements();
        long macKey = coinAndMacKey[2];

        // 2. get the prp for generating many random numbers
        int parallelNum = parallel ? ForkJoinPool.getCommonPoolParallelism() : 1;
        byte[] randKey = LongUtils.longArrayToByteArray(Arrays.copyOf(coinAndMacKey, 2));
        Prp[] prp = IntStream.range(0, parallelNum).mapToObj(j -> {
            Prp tmp = PrpFactory.createInstance(getEnvType());
            tmp.setKey(randKey);
            return tmp;
        }).toArray(Prp[]::new);

        // 3. generate multiple random coins α_i, and compute sum(α_i · z) and sum(α_i · (r · z_i))
        long countIndex = 0;
        long[] sumValue = new long[]{0L, 0L};
        if (!memoryBuffer.isEmpty()) {
            for (TripletRpLongMacVector tmp : memoryBuffer) {
                byte[] randByte = PrpUtils.generateRandBytes(prp, countIndex, tmp.getNum() << 3);
                countIndex += tmp.getNum();
                PlainLongVector randLong = (PlainLongVector) create(true, LongUtils.byteArrayToLongArray(randByte));
                muli(tmp, randLong);
                for (int k = 0; k < 2; k++) {
                    sumValue[k] += tmp.getMacVec()[k].sum() - tmp.getVectors()[k].sum() * macKey;
                }
            }
            memoryBuffer.clear();
        }
        while (fileBufferIndexes[1] > fileBufferIndexes[0]) {
            int readLen = Math.min(fileBufferIndexes[1] - fileBufferIndexes[0], singleVerifyThreshold);
            TripletRpLongMacVector[] data = readBufferFile(readLen);
            byte[] randByte = PrpUtils.generateRandBytes(prp, countIndex, readLen * maxBufferElementLen * 8);
            countIndex += (long) readLen * maxBufferElementLen;
            PlainLongVector[] allR = (PlainLongVector[]) create(true, LongUtils.byteArrayToLongArray(randByte))
                .split(IntStream.range(0, readLen).map(i -> maxBufferElementLen).toArray());

            IntStream intStream = parallel ? IntStream.range(0, readLen).parallel() : IntStream.range(0, readLen);
            long[][] tmp = intStream.mapToObj(i -> {
                muli(data[i], allR[i]);
                return IntStream.range(0, 2).mapToLong(k ->
                    data[i].getMacVec()[k].sum() - data[i].getVectors()[k].sum() * macKey).toArray();
            }).toArray(long[][]::new);
            for (long[] longs : tmp) {
                sumValue[0] += longs[0];
                sumValue[1] += longs[1];
            }
        }
        compareView4Zero(64, TripletRpLongMacVector.create(new long[][]{new long[]{sumValue[0]}, new long[]{sumValue[1]}}));
        resetMac();
    }

    public void intoBuffer(TripletRpLongMacVector[] unverifiedData) {
        // if buffer is empty or the last one is full
        if (memoryBuffer.isEmpty() || validNumOfLastBuffer == maxBufferElementLen) {
            this.addEmptyBuffer();
        }
        for (TripletRpLongMacVector unverifiedDatum : unverifiedData) {
            // the following code essentially copy the unverified multiplication result into new fixed-length vectors
            // set values of buffer from start to end, opposite to the z2
            int singleInputArrayLen = unverifiedDatum.getNum();
            while (singleInputArrayLen > 0) {
                TripletRpLongMacVector currentBuffer = memoryBuffer.get(memoryBuffer.size() - 1);
                int lastGroupCapLen = maxBufferElementLen - validNumOfLastBuffer;
                int copyLen = Math.min(lastGroupCapLen, singleInputArrayLen);
                int sourceCopyStartPos = singleInputArrayLen - copyLen;
                currentBuffer.setElements(unverifiedDatum, sourceCopyStartPos, validNumOfLastBuffer, copyLen);

                validNumOfLastBuffer += copyLen;
                singleInputArrayLen = sourceCopyStartPos;
                if (copyLen == lastGroupCapLen) {
                    this.addEmptyBuffer();
                }
            }
        }
        if (memoryBufferThreshold < memoryBuffer.size()) {
            int saveNum = memoryBuffer.size() - memoryBufferThreshold;
            TripletRpLongMacVector[] saveData = IntStream.range(0, saveNum).mapToObj(i ->
                memoryBuffer.remove(0)).toArray(TripletRpLongMacVector[]::new);
            writeIntoFile(saveData);
        }
    }

    /**
     * generate the mac for data with the current mac key
     */
    public void genMac(TripletLongVector[] data) {
        int[] indexes = IntStream.range(0, data.length)
            .filter(i -> !(data[i] instanceof TripletRpLongMacVector)
                || ((TripletRpLongMacVector) data[i]).getMacIndex() != currentMacIndex).toArray();
        if (indexes == null || indexes.length == 0) {
            return;
        }
        TripletRpLongMacVector[] noMacData = Arrays.stream(indexes).mapToObj(i ->
            TripletRpLongMacVector.create((TripletRpLongVector) data[i], false)).toArray(TripletRpLongMacVector[]::new);
        int[] dataNum = Arrays.stream(noMacData).mapToInt(TripletRpLongVector::getNum).toArray();
        int totalNum = Arrays.stream(dataNum).sum();

        LongVector[] r = crProvider.randZeroZl64Vector(totalNum).split(dataNum);
        IntStream intStream = parallel ? IntStream.range(0, noMacData.length).parallel() : IntStream.range(0, noMacData.length);
        long sumMac = shareMacKey[0] + shareMacKey[1];
        intStream.forEach(i -> {
            long[] tmp = Arrays.stream(noMacData[i].getVectors()[0].getElements()).map(each -> each * sumMac).toArray();
            long[] d2 = noMacData[i].getVectors()[1].getElements();
            IntStream.range(0, tmp.length).forEach(j -> tmp[j] += d2[j] * shareMacKey[0]);
            r[i].addi(LongVector.create(tmp));
        });

        sendLongVectors(PtoStep.MUL_OP.ordinal(), leftParty(), r);
        LongVector[] rightData = receiveLongVectors(PtoStep.MUL_OP.ordinal(), rightParty());
        MathPreconditions.checkEqual("sendData.length", "rightData.length", r.length, rightData.length);
        extraInfo++;
        IntStream.range(0, noMacData.length).forEach(i -> {
            noMacData[i].setMacIndex(currentMacIndex);
            noMacData[i].setMacVec(r[i], rightData[i]);
        });

        intoBuffer(noMacData);
        IntStream.range(0, indexes.length).forEach(i -> data[indexes[i]] = noMacData[i]);
    }

    /**
     * add an empty memory buffer, and reset the validByteNumOfLastBuffer
     */
    private void addEmptyBuffer() {
        memoryBuffer.add(TripletRpLongMacVector.createEmpty(maxBufferElementLen));
        validNumOfLastBuffer = 0;
    }

    /**
     * write the memory buffer into files
     *
     * @param data to be stored in files
     */
    private void writeIntoFile(TripletRpLongMacVector[] data) {
        IntStream intStream = parallel ? IntStream.range(0, data.length).parallel() : IntStream.range(0, data.length);
        intStream.forEach(each -> {
            int index = each + fileBufferIndexes[1];
            String filePath = bufferPath + File.separator + index + "_" + selfId + "_zl64_mac.txt";
            long[][] writeData = new long[data.length << 2][];
            for (int i = 0, j = 2; i < data.length; i++, j++) {
                writeData[i] = data[each].getVectors()[i].getElements();
                writeData[j] = data[each].getMacVec()[i].getElements();
            }
            FileUtils.writeFile(writeData, filePath);
        });
        fileBufferIndexes[1] += data.length;
    }

    /**
     * read the files into the memory buffer into bitVectors
     *
     * @param batchNum how many files should be read and deleted
     */
    private TripletRpLongMacVector[] readBufferFile(int batchNum) {
        IntStream intStream = parallel ? IntStream.range(0, batchNum).parallel() : IntStream.range(0, batchNum);
        TripletRpLongMacVector[] res = intStream.mapToObj(i -> {
            int index = i + fileBufferIndexes[0];
            long[][] tmp = FileUtils.readFileIntoLongMatrix(bufferPath + File.separator + index + "_" + selfId + "_zl64_mac.txt", true);
            assert tmp.length == 4;
            return TripletRpLongMacVector.create(currentMacIndex, Arrays.copyOf(tmp, 2), Arrays.copyOfRange(tmp, 2, 4));
        }).toArray(TripletRpLongMacVector[]::new);
        fileBufferIndexes[0] += batchNum;
        return res;
    }
}
