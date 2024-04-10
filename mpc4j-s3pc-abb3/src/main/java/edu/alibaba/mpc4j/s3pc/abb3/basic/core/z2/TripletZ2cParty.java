package edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.AbbCoreParty;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Z2c Party for three-party secret sharing
 *
 * @author Feng Han
 * @date 2023/12/15
 */
public interface TripletZ2cParty extends AbbCoreParty, MpcZ2cParty {
    /**
     * transpose the bit matrix
     *
     * @param data matrix in row
     * @return transposed matrix
     */
    default TripletZ2Vector[] matrixTranspose(TripletZ2Vector[] data) {
        int vecNumOfEachShare = data[0].getBitVectors().length;
        BitVector[][] rows = IntStream.range(0, vecNumOfEachShare).mapToObj(i ->
                Arrays.stream(data).map(x -> x.getBitVectors()[i]).toArray(BitVector[]::new))
            .toArray(BitVector[][]::new);

        BitVector[][] transRes = Arrays.stream(rows).map(array -> {
            byte[][] tmp = ZlDatabase.create(getEnvType(), getParallel(), array).getBytesData();
            return Arrays.stream(tmp).map(each -> BitVectorFactory.create(data.length, each)).toArray(BitVector[]::new);
        }).toArray(BitVector[][]::new);
        return IntStream.range(0, transRes[0].length).mapToObj(i ->
                (TripletZ2Vector) create(data[0].isPlain(), Arrays.stream(transRes).map(x -> x[i]).toArray(BitVector[]::new)))
            .toArray(TripletZ2Vector[]::new);
    }

    /**
     * inits the protocol.
     */
    void init();

    /**
     * create shared zero vectors
     */
    TripletZ2Vector createShareZeros(int bitNums);

    /**
     * Shares its own vector.
     *
     * @param xi the vector to be shared.
     * @return the shared vector.
     */
    @Override
    default TripletZ2Vector shareOwn(BitVector xi) throws MpcAbortException {
        return shareOwn(new BitVector[]{xi})[0];
    }

    /**
     * Shares its own vectors。
     *
     * @param xiArray the vectors to be shared.
     * @return the shared vectors.
     */
    @Override
    TripletZ2Vector[] shareOwn(BitVector[] xiArray) throws MpcAbortException;

    /**
     * Shares other's vector.
     *
     * @param bitNum the number of bits to be shared.
     * @return the shared vector.
     */
    default TripletZ2Vector shareOther(int bitNum, Party party) throws MpcAbortException {
        return shareOther(new int[]{bitNum}, party)[0];
    }

    /**
     * Shares other's vectors.
     *
     * @param bitNums the number of bits for each vector to be shared.
     * @return the shared vectors.
     */
    TripletZ2Vector[] shareOther(int[] bitNums, Party party) throws MpcAbortException;

    /**
     * Reveals its own vectors.
     *
     * @param xiArray the shared vectors.
     * @return the revealed vectors.
     * @throws MpcAbortException the protocol failure aborts.
     */
    @Override
    BitVector[] revealOwn(MpcZ2Vector[] xiArray) throws MpcAbortException;

    /**
     * Reveals its own vectors.
     *
     * @param xiArray the shared vectors.
     * @return the revealed vectors.
     * @throws MpcAbortException the protocol failure aborts.
     */
    default BitVector revealOwn(MpcZ2Vector xiArray) throws MpcAbortException {
        return revealOwn(new MpcZ2Vector[]{xiArray})[0];
    }

    /**
     * Reveals other's vectors.
     *
     * @param xiArray the shared vectors.
     */
    void revealOther(MpcZ2Vector[] xiArray, Party party) throws MpcAbortException;

    /**
     * Reveals other's vectors.
     *
     * @param xiArray the shared vectors.
     */
    default void revealOther(MpcZ2Vector xiArray, Party party) throws MpcAbortException {
        revealOther(new MpcZ2Vector[]{xiArray}, party);
    }

    @Override
    default BitVector[] open(MpcZ2Vector[] xiArray) throws MpcAbortException{
        TripletZ2Vector[] data = Arrays.stream(xiArray).map(each -> {
            assert each instanceof TripletZ2Vector;
            return (TripletZ2Vector) each;
        }).toArray(TripletZ2Vector[]::new);
        return open(data);
    }

    /**
     * open shared vectors.
     *
     * @param xiArray the clear vectors.
     */
    BitVector[] open(TripletZ2Vector[] xiArray) throws MpcAbortException;

    /**
     * AND operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x & y.
     */
    @Override
    default TripletZ2Vector and(MpcZ2Vector xi, MpcZ2Vector yi) {
        return and(new MpcZ2Vector[]{xi}, new MpcZ2Vector[]{yi})[0];
    }

    /**
     * Vector AND operations. xi = xi & yi
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     */
    void andi(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray);

    /**
     * AND operation. xi = xi & yi
     *
     * @param xi xi.
     * @param yi yi.
     */
    default void andi(MpcZ2Vector xi, MpcZ2Vector yi) {
        andi(new MpcZ2Vector[]{xi}, new MpcZ2Vector[]{yi});
    }

    /**
     * Vector AND operations.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] & y[i].
     */
    @Override
    TripletZ2Vector[] and(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray);

    /**
     * XOR operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x ^ y.
     */
    @Override
    TripletRpZ2Vector xor(MpcZ2Vector xi, MpcZ2Vector yi);

    /**
     * Vector XOR operation.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] ^ y[i].
     */
    @Override
    default TripletRpZ2Vector[] xor(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) {
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        IntStream intStream = getParallel() ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        return intStream.mapToObj(i -> xor(xiArray[i], yiArray[i])).toArray(TripletRpZ2Vector[]::new);
    }

    /**
     * XOR operation. x = x ^ y.
     *
     * @param xi xi.
     * @param yi yi.
     */
    @Override
    void xori(MpcZ2Vector xi, MpcZ2Vector yi);

    /**
     * XOR operation. x = x ^ y.
     *
     * @param xiArray xi.
     * @param yiArray yi.
     */
    default void xori(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) {
        MathPreconditions.checkEqual("xiArray.length", "yi.length", xiArray.length, yiArray.length);
        IntStream intStream = getParallel() ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        intStream.forEach(i -> {
            assert !xiArray[i].isPlain();
            xori(xiArray[i], yiArray[i]);
        });
    }

    /**
     * NOT operation.
     *
     * @param xi xi.
     * @return zi, such that z = !x.
     */
    @Override
    MpcZ2Vector not(MpcZ2Vector xi);

    /**
     * Vector NOT operation.
     *
     * @param xiArray xi array.
     * @return zi array, such that for each j, z[i] = !x[i].
     */
    @Override
    default MpcZ2Vector[] not(MpcZ2Vector[] xiArray) {
        Stream<MpcZ2Vector> stream = getParallel() ? Arrays.stream(xiArray).parallel() : Arrays.stream(xiArray);
        return stream.map(this::not).toArray(MpcZ2Vector[]::new);
    }

    /**
     * NOT operation. x = !x.
     *
     * @param xi xi.
     */
    @Override
    void noti(MpcZ2Vector xi);

    /**
     * NOT operation. x[i] = !x[i].
     */
    default void noti(MpcZ2Vector[] xiArray){
        Stream<MpcZ2Vector> stream = getParallel() ? Arrays.stream(xiArray).parallel() : Arrays.stream(xiArray);
        stream.forEach(this::noti);
    }

    /**
     * OR operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x | y.
     */
    @Override
    default TripletZ2Vector or(MpcZ2Vector xi, MpcZ2Vector yi) {
        return xor(xor(xi, yi), and(xi, yi));
    }

    /**
     * Vector OR operation.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = z[i] | y[i].
     */
    @Override
    default TripletZ2Vector[] or(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) {
        return xor(xor(xiArray, yiArray), and(xiArray, yiArray));
    }

    @Override
    default void verifyMul() throws MpcAbortException {
        // do nothing if semi-honest
    }

    /**
     * check the unverified multiplication calculations
     *
     * @throws MpcAbortException if the protocol is abort.
     */
    void checkUnverified() throws MpcAbortException;

    /**
     * verify the data is the share of zeros
     *
     * @throws MpcAbortException if the protocol is abort.
     */
    default void compareView4Zero(TripletZ2Vector... data) throws MpcAbortException {

    }

    /**
     * Shares other's vector.
     *
     * @param bitNum the number of bits to be shared.
     * @return the shared vector.
     */
    @Override
    default TripletZ2Vector shareOther(int bitNum) {
        throw new RuntimeException("should not invoke this method");
    }

    /**
     * Shares other's vectors.
     *
     * @param bitNums the number of bits for each vector to be shared.
     * @return the shared vectors.
     */
    @Override
    default TripletZ2Vector[] shareOther(int[] bitNums) {
        throw new RuntimeException("should not invoke this method");
    }

    /**
     * Reveals other's vectors.
     *
     * @param xiArray the shared vectors.
     */
    @Override
    default void revealOther(MpcZ2Vector xiArray) {
        throw new RuntimeException("should not invoke this method");
    }

    /**
     * Reveals other's vectors.
     *
     * @param xiArray the shared vectors.
     */
    @Override
    default void revealOther(MpcZ2Vector[] xiArray) {
        throw new RuntimeException("should not invoke this method");
    }

    /**
     * Creates an empty vector.
     *
     * @param plain the plain state.
     * @return a vector.
     */
    @Override
    default MpcZ2Vector createEmpty(boolean plain) {
        throw new RuntimeException("should not invoke this method");
    }

    /**
     * merges the vector.
     *
     * @param vectors vectors.
     * @return the merged vector.
     */
    @Override
    default MpcZ2Vector merge(MpcZ2Vector[] vectors) {
        throw new RuntimeException("should not invoke this method");
    }

    /**
     * splits the vector.
     *
     * @param mergeVector the merged vector.
     * @param bitNums     bits for each of the split vector.
     * @return the split vector.
     */
    @Override
    default MpcZ2Vector[] split(MpcZ2Vector mergeVector, int[] bitNums) {
        throw new RuntimeException("should not invoke this method");
    }
}
