package edu.alibaba.mpc4j.s2pc.pir;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * PIR utils.
 *
 * @author Liqiang Peng
 * @date 2022/8/1
 */
public class PirUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(PirUtils.class);

    /**
     * secure random
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * private constructor.
     */
    private PirUtils() {
        // empty
    }

    /**
     * generate random bytebuffer sets.
     *
     * @param serverSetSize server set size.
     * @param clientSetSize client set size.
     * @param repeatTime    repeat time.
     * @return string sets.
     */
    public static List<Set<ByteBuffer>> generateByteBufferSets(int serverSetSize, int clientSetSize, int repeatTime) {
        assert serverSetSize >= 1 : "server must have at least 1 elements";
        assert clientSetSize >= 1 : "client must have at least 1 elements";
        assert repeatTime >= 1 : "repeat time must be greater than or equal to 1: " + repeatTime;
        // create server set
        Set<ByteBuffer> serverSet = IntStream.range(0, serverSetSize)
            .mapToObj(index -> ByteBuffer.wrap(("ID_" + index).getBytes()))
            .collect(Collectors.toSet());
        List<ByteBuffer> serverList = new ArrayList<>(serverSet);
        // create client set
        List<Set<ByteBuffer>> clientSets = IntStream.range(0, repeatTime)
            .mapToObj(repeatIndex -> {
                if (clientSetSize > 1) {
                    int matchedItemSize = clientSetSize / 2;
                    Set<ByteBuffer> clientSet = new HashSet<>(clientSetSize);
                    for (int index = 0; index < matchedItemSize; index++) {
                        clientSet.add(serverList.get(index));
                    }
                    for (int index = matchedItemSize; index < clientSetSize; index++) {
                        clientSet.add(ByteBuffer.wrap(("ID_" + index + "_DISTINCT").getBytes()));
                    }
                    return clientSet;
                } else {
                    Set<ByteBuffer> clientSet = new HashSet<>(clientSetSize);
                    int index = SECURE_RANDOM.nextInt(serverSetSize);
                    if (SECURE_RANDOM.nextBoolean()) {
                        clientSet.add(serverList.get(index));
                    } else {
                        clientSet.add(ByteBuffer.wrap(("ID_" + index + "_DISTINCT").getBytes()));
                    }
                    return clientSet;
                }
            })
            .collect(Collectors.toCollection(ArrayList::new));
        List<Set<ByteBuffer>> results = new ArrayList<>(2);
        results.add(serverSet);
        results.addAll(clientSets);
        return results;
    }

    /**
     * generate keyword label map.
     *
     * @param keywordSet      keyword set.
     * @param labelByteLength label byte length.
     * @return keyword label map.
     */
    public static Map<ByteBuffer, byte[]> generateKeywordByteBufferLabelMap(Set<ByteBuffer> keywordSet,
                                                                                int labelByteLength) {
        return keywordSet.stream()
            .collect(Collectors.toMap(
                keyword -> keyword,
                keyword -> {
                    byte[] label = new byte[labelByteLength];
                    SECURE_RANDOM.nextBytes(label);
                    return label;
                }
            ));
    }

    /**
     * Creates a random database.
     *
     * @param elementSize      database size.
     * @param elementBitLength element bit length.
     * @return random database.
     */
    public static NaiveDatabase generateDataBase(int elementSize, int elementBitLength) {
        return NaiveDatabase.createRandom(elementBitLength, elementSize, SECURE_RANDOM);
    }

    /**
     * generate random element array.
     *
     * @param elementSize      element size.
     * @param elementBitLength element bit length.
     * @return random element array.
     */
    public static byte[][] generateElementArray(int elementSize, int elementBitLength) {
        int elementByteLength = CommonUtils.getByteLength(elementBitLength);
        return IntStream.range(0, elementSize)
            .mapToObj(i -> BytesUtils.randomByteArray(elementByteLength, elementBitLength, SECURE_RANDOM))
            .toArray(byte[][]::new);
    }

    /**
     * generate a random retrieval index.
     *
     * @param elementSize element size.
     * @return a random retrieval index.
     */
    public static int generateRetrievalIndex(int elementSize) {
        return SECURE_RANDOM.nextInt(elementSize);
    }

    /**
     * generate random retrieval index set.
     *
     * @param elementSize   element size.
     * @param retrievalSize retrieval size.
     * @return random retrieval index set.
     */
    public static Set<Integer> generateRetrievalIndexSet(int elementSize, int retrievalSize) {
        Set<Integer> indexSet = new HashSet<>();
        while (indexSet.size() < retrievalSize) {
            int index = SECURE_RANDOM.nextInt(elementSize);
            indexSet.add(index);
        }
        return indexSet;
    }

    /**
     * server bytes prefix
     */
    public static final String BYTES_SERVER_PREFIX = "BYTES_SERVER";
    /**
     * client bytes prefix
     */
    public static final String BYTES_CLIENT_PREFIX = "BYTES_CLIENT";

    /**
     * generate bytes input files.
     *
     * @param setSize          set size.
     * @param elementBitLength element bit length.
     * @throws IOException create files failed.
     */
    public static void generateBytesInputFiles(int setSize, int elementBitLength) throws IOException {
        MathPreconditions.checkPositive("elementBitLength", elementBitLength);
        File serverInputFile = new File(getServerFileName(BYTES_SERVER_PREFIX, setSize, elementBitLength));
        if (serverInputFile.exists()) {
            return;
        }
        LOGGER.info("Lost some / all files, generate byte[] set files.");
        if (serverInputFile.exists()) {
            LOGGER.info("Delete server byte[] set file.");
            Preconditions.checkArgument(
                serverInputFile.delete(), "Fail to delete file: %s", serverInputFile.getName()
            );
        }
        byte[][] elementArray = generateElementArray(setSize, elementBitLength);
        FileWriter serverFileWriter = new FileWriter(serverInputFile);
        PrintWriter serverPrintWriter = new PrintWriter(serverFileWriter, true);
        IntStream.range(0, setSize)
            .mapToObj(i -> Hex.toHexString(elementArray[i]))
            .forEach(serverPrintWriter::println);
        serverPrintWriter.close();
        serverFileWriter.close();
    }

    /**
     * generate retrieval index input files.
     *
     * @param retrievalSize retrieval size.
     * @throws IOException create files failed.
     */
    public static void generateIndexInputFiles(int elementSize, int retrievalSize)
        throws IOException {
        MathPreconditions.checkPositive("retrievalSize", retrievalSize);
        File clientInputFile = new File(getClientFileName(BYTES_CLIENT_PREFIX, retrievalSize));
        if (clientInputFile.exists()) {
            return;
        }
        LOGGER.info("Lost some / all files, generate byte[] set files.");
        if (clientInputFile.exists()) {
            LOGGER.info("Delete server byte[] set file.");
            Preconditions.checkArgument(
                clientInputFile.delete(), "Fail to delete file: %s", clientInputFile.getName()
            );
        }
        Set<Integer> retrievalIndexSet = generateRetrievalIndexSet(elementSize, retrievalSize);
        FileWriter clientFileWriter = new FileWriter(clientInputFile);
        PrintWriter clientPrintWriter = new PrintWriter(clientFileWriter, true);
        retrievalIndexSet.stream()
            .map(IntUtils::intToByteArray)
            .map(Hex::toHexString)
            .forEach(clientPrintWriter::println);
        clientPrintWriter.close();
        clientPrintWriter.close();
    }

    /**
     * return server file name.
     *
     * @param prefix           prefix.
     * @param setSize          set size.
     * @param elementBitLength element bit length.
     * @return server file name.
     */
    public static String getServerFileName(String prefix, int setSize, int elementBitLength) {
        return prefix + "_" + elementBitLength + "_" + setSize + ".input";
    }

    /**
     * return client file name.
     *
     * @param prefix           prefix.
     * @param setSize          set size.
     * @return client file name.
     */
    public static String getClientFileName(String prefix, int setSize) {
        return prefix + "_" + setSize + ".input";
    }

    /**
     * return element size of plaintext.
     *
     * @param elementByteLength element byte length.
     * @param polyModulusDegree poly modulus degree.
     * @param coeffBitLength    coeff bit length.
     * @return element size of plaintext.
     */
    public static int elementSizeOfPlaintext(int elementByteLength, int polyModulusDegree, int coeffBitLength) {
        int coeffSizeOfElement = coeffSizeOfElement(elementByteLength, coeffBitLength);
        int elementSizeOfPlaintext = polyModulusDegree / coeffSizeOfElement;
        assert elementSizeOfPlaintext > 0 :
            "N should be larger than the of coefficients needed to represent a database element";
        return elementSizeOfPlaintext;
    }

    /**
     * return coeff size of element.
     *
     * @param elementByteLength element byte length.
     * @param coeffBitLength    coeff bit length.
     * @return coeff size of element.
     */
    public static int coeffSizeOfElement(int elementByteLength, int coeffBitLength) {
        return CommonUtils.getUnitNum(Byte.SIZE * elementByteLength, coeffBitLength);
    }

    /**
     * convert byte array to coeff array.
     *
     * @param limit     coeff bit length.
     * @param offset    offset.
     * @param size      size of byte array.
     * @param byteArray byte array.
     * @return coeff array.
     */
    public static long[] convertBytesToCoeffs(int limit, int offset, int size, byte[] byteArray) {
        int longArraySize = CommonUtils.getUnitNum(Byte.SIZE * size, limit);
        long[] longArray = new long[longArraySize];
        int room = limit;
        int flag = 0;
        for (int i = 0; i < size; i++) {
            int src = byteArray[i+offset];
            if (src < 0) {
                src &= 0xFF;
            }
            int rest = Byte.SIZE;
            while (rest != 0) {
                if (room == 0) {
                    flag++;
                    room = limit;
                }
                int shift = Math.min(room, rest);
                long temp = longArray[flag] << shift;
                longArray[flag] = temp | (src >> (Byte.SIZE - shift));
                int remain = (1 << (Byte.SIZE - shift)) - 1;
                src = (src & remain) << shift;
                room -= shift;
                rest -= shift;
            }
        }
        longArray[flag] = longArray[flag] << room;
        return longArray;
    }


    /**
     * convert coeff array to byte array.
     *
     * @param coeffArray coeff array.
     * @param logt       coeff bit length.
     * @return byte array.
     */
    public static byte[] convertCoeffsToBytes(long[] coeffArray, int logt) {
        int len = CommonUtils.getUnitNum(coeffArray.length * logt, Byte.SIZE);
        byte[] byteArray = new byte[len];
        int room = Byte.SIZE;
        int j = 0;
        for (long l : coeffArray) {
            long src = l;
            int rest = logt;
            while (rest != 0 && j < byteArray.length) {
                int shift = Math.min(room, rest);
                byteArray[j] = (byte) (byteArray[j] << shift);
                byteArray[j] = (byte) (byteArray[j] | (src >> (logt - shift)));
                src = src << shift;
                room -= shift;
                rest -= shift;
                if (room == 0) {
                    j++;
                    room = Byte.SIZE;
                }
            }
        }
        return byteArray;
    }

    /**
     * compute indices in each dimension.
     *
     * @param retrievalIndex retrieval index.
     * @param dimensionSize  dimension size.
     * @return indices in each dimension.
     */
    public static int[] computeIndices(int retrievalIndex, int[] dimensionSize) {
        long product = Arrays.stream(dimensionSize).asLongStream().reduce(1, (a, b) -> a * b);
        int[] indices = new int[dimensionSize.length];
        for (int i = 0; i < dimensionSize.length; i++) {
            product /= dimensionSize[i];
            int ji = (int) (retrievalIndex / product);
            indices[i] = ji;
            retrievalIndex -= ji * product;
        }
        return indices;
    }

    /**
     * return the bit length of the input integer.
     *
     * @param input input.
     * @return bit length.
     */
    public static int getBitLength(long input) {
        int count = 0;
        while (input != 0) {
            count++;
            input /= 2;
        }
        return count;
    }

    /**
     * return next power of two of the input integer.
     *
     * @param input input.
     * @return next power of two.
     */
    public static int getNextPowerOfTwo(int input) {
        if ((input & (input - 1)) == 0) {
            return input;
        }
        int numberOfBits = getBitLength(input);
        return (1 << numberOfBits);
    }

    /**
     * rotate vector column.
     *
     * @param coeffs coeff vector.
     * @return rotated coeff vector.
     */
    public static long[] rotateVectorCol(long[] coeffs) {
        int rowSize = coeffs.length / 2;
        long[] result = new long[coeffs.length];
        IntStream.range(0, rowSize).forEach(i -> {
            result[i] = coeffs[rowSize + i];
            result[rowSize + i] = coeffs[i];
        });
        return result;
    }

    /**
     * plaintext rotate.
     *
     * @param coeffs coefficients.
     * @param offset offset.
     * @return rotated plaintext.
     */
    public static long[] plaintextRotate(long[] coeffs, int offset) {
        int rowCount = coeffs.length / 2;
        offset = offset % rowCount;
        long[] rotatedCoeffs = new long[coeffs.length];
        for (int i = 0; i < rowCount; i++) {
            rotatedCoeffs[(i + offset) % rowCount] = coeffs[i];
            rotatedCoeffs[(i + offset) % rowCount + rowCount] = coeffs[i + rowCount];
        }
        return rotatedCoeffs;
    }

    /**
     * return length of each dimension.
     *
     * @param elementSize element size.
     * @return length of each dimension.
     */
    public static int[] computeDimensionLength(int elementSize, int dimension) {
        int[] dimensionLength = IntStream.range(0, dimension)
            .map(i -> (int) Math.max(2, Math.floor(Math.pow(elementSize, 1.0 / dimension))))
            .toArray();
        int product = 1;
        int j = 0;
        // if plaintext_num is not a d-power
        if (dimensionLength[0] != Math.pow(elementSize, 1.0 / dimension)) {
            while (product < elementSize && j < dimension) {
                product = 1;
                dimensionLength[j++]++;
                for (int i = 0; i < dimension; i++) {
                    product *= dimensionLength[i];
                }
            }
        }
        return dimensionLength;
    }

    /**
     * return length of each dimension.
     *
     * @param elementSize             element size.
     * @param firstDimensionSize      first dimension size.
     * @param subsequentDimensionSize subsequent dimension size.
     * @return length of each dimension.
     */
    public static int[] computeDimensionLength(int elementSize, int firstDimensionSize, int subsequentDimensionSize) {
        List<Integer> dimensionLength = new ArrayList<>();
        dimensionLength.add(firstDimensionSize);
        int product = firstDimensionSize;
        for (int i = elementSize / firstDimensionSize; i >= subsequentDimensionSize; i /= subsequentDimensionSize) {
            dimensionLength.add(subsequentDimensionSize);
            product *= subsequentDimensionSize;
        }
        int dimensionSize = dimensionLength.size();
        int[] dimensionArray = IntStream.range(0, dimensionSize).map(dimensionLength::get).toArray();
        while (product < elementSize) {
            dimensionArray[dimensionSize - 1]++;
            product = 1;
            product *= Arrays.stream(dimensionArray, 0, dimensionSize).reduce(1, (a, b) -> a * b);
        }
        if (dimensionSize == 1 && dimensionArray[0] > firstDimensionSize) {
            dimensionArray = new int[] {firstDimensionSize, subsequentDimensionSize};
        }
        return dimensionArray;
    }

    /**
     * Find smallest l, m such that l*m >= num * d and d divides l, where d is
     * the number of Z_p elements per DB entry determined by bit-length and p.
     *
     * @param num database size.
     * @param d   Z_p element num.
     * @return l and m.
     */
    public static int[] approxSquareDatabaseDims(int num, int d) {
        MathPreconditions.checkPositive("num", num);
        long rows = (long) Math.max(2, Math.ceil(Math.sqrt(d * num)));
        long rem = rows % d;
        if (rem != 0) {
            rows += d - rem;
        }
        long cols = (long) Math.ceil((double) d * num / rows);
        return new int[]{Math.toIntExact(rows), Math.toIntExact(cols)};
    }

    /**
     * compute powers.
     *
     * @param zp64      zp64.
     * @param base      base.
     * @param exponents exponents.
     * @return powers.
     */
    public static long[][] computePowers(Zp64 zp64, long[] base, int[] exponents) {
        long[][] result = new long[exponents.length][];
        assert exponents[0] == 1;
        result[0] = base;
        for (int i = 1; i < exponents.length; i++) {
            long[] temp = new long[base.length];
            for (int j = 0; j < base.length; j++) {
                temp[j] = zp64.pow(base[j], exponents[i]);
            }
            result[i] = temp;
        }
        return result;
    }
}
