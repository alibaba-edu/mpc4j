package edu.alibaba.mpc4j.common.structure.fusefilter;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;

/**
 * byte fuse filter with arity = 3. The implementation comes from
 * <a href="https://github.com/FastFilter/fastfilter_java/blob/master/fastfilter/src/main/java/org/fastfilter/xor/XorBinaryFuse8.java">XorBinaryFuse8</a>.
 *
 * @author Weiran Liu
 * @date 2024/7/25
 */
public class Arity3ByteFuseFilter<T> extends AbstractArity3ByteFusePosition<T> implements ByteFuseFilter<T> {
    /**
     * storage
     */
    private final byte[][] storage;

    public Arity3ByteFuseFilter(EnvType envType, Map<T, byte[]> keyValueMap, int valueByteLength) {
        this(envType, keyValueMap, valueByteLength, new SecureRandom());
    }

    public Arity3ByteFuseFilter(EnvType envType, Map<T, byte[]> keyValueMap, int valueByteLength, SecureRandom secureRandom) {
        super(envType, keyValueMap.size(), valueByteLength);
        // check value byte length
        for (T key : keyValueMap.keySet()) {
            byte[] value = keyValueMap.get(key);
            MathPreconditions.checkEqual(
                "value_byte_length", "value_byte_length for " + key, valueByteLength, value.length
            );
        }
        storage = new byte[filterLength][valueByteLength];
        addAll(keyValueMap, secureRandom);
    }

    private void addAll(Map<T, byte[]> keyValueMap, SecureRandom secureRandom) {
        int size = keyValueMap.size();
        TLongObjectMap<byte[]> hashValueMap = new TLongObjectHashMap<>(size + 1);
        // the stack P that stores the hash of the key x
        long[] reverseOrder = new long[size + 1];
        // the stack P that stores the location i of the key x
        byte[] reverseH = new byte[size];
        // the pointer of the stack P
        int reverseOrderPos;

        // the lowest 2 bits are the h index (0, 1, or 2), the highest 6 bites are used to store counting
        byte[] t2count = new byte[filterLength];
        long[] t2hash = new long[filterLength];
        // Allocate an array c of sets having the same size as H. The sets are initially empty.
        int[] alone = new int[filterLength];
        int repeat = 0;
        // (h_0(x), h_1(x), h_2(x), h_0(x), h_1(x))
        int[] h012 = new int[5];
        // block is the nearest value that is greater than or equal to segmentCount and is power-of-2.
        int blockBits = 1;
        while ((1 << blockBits) < segmentCount) {
            blockBits++;
        }
        int block = 1 << blockBits;

        while (true) {
            secureRandom.nextBytes(seed);
            hash.setKey(seed);
            reverseOrder[size] = 1;
            // start position for each segment
            int[] startPos = new int[block];
            for (int i = 0; i < 1 << blockBits; i++) {
                startPos[i] = (int) ((long) i * size / block);
            }
            // counting sort
            for (T key : keyValueMap.keySet()) {
                long hash = hash(key);
                hashValueMap.put(hash, keyValueMap.get(key));
                int segmentIndex = (int) (hash >>> (64 - blockBits));
                // We only overwrite when the hash was zero. Zero hash values may be misplaced (unlikely).
                while (reverseOrder[startPos[segmentIndex]] != 0) {
                    segmentIndex++;
                    segmentIndex &= (1 << blockBits) - 1;
                }
                reverseOrder[startPos[segmentIndex]] = hash;
                startPos[segmentIndex]++;
            }
            // count mask is used to label if there is a bin with count overflow (greater than 2^5).
            byte countMask = 0;
            // Scan through the keys x in set S in order, and add x to the sets c[h_0(x)], c[h_1(x)], c[h_2(x)]
            for (int i = 0; i < size; i++) {
                long hash = reverseOrder[i];
                for (int hi = 0; hi < ARITY; hi++) {
                    int index = getHashFromHash(hash, hi);
                    // add 1 at the highest 6 bits.
                    t2count[index] += 4;
                    // store h index at the lowest 2 bits.
                    t2count[index] ^= (byte) hi;
                    // store hash of the key x
                    t2hash[index] ^= hash;
                    countMask |= t2count[index];
                }
            }
            if (countMask < 0) {
                // we have a possible counter overflow
                continue;
            }

            reverseOrderPos = 0;
            int alonePos = 0;
            // Scan through the array c in order, and each time a singleton is found, append the location to a stack(Q).
            for (int i = 0; i < filterLength; i++) {
                alone[alonePos] = i;
                int inc = (t2count[i] >> 2) == 1 ? 1 : 0;
                alonePos += inc;
            }
            // while stack Q is not empty do
            while (alonePos > 0) {
                // Pop a location i from the stack Q
                alonePos--;
                int index = alone[alonePos];
                // if c[i] is a singleton then
                if ((t2count[index] >> 2) == 1) {
                    // Append the key x contained in c[i] and its location i to the stack P
                    long hash = t2hash[index];
                    byte found = (byte) (t2count[index] & 3);
                    reverseH[reverseOrderPos] = found;
                    reverseOrder[reverseOrderPos] = hash;
                    // Remove the key x from lists c[h_0(x)], c[h_1(x)], c[h_2(x)]
                    // we only need to remove c[h_{i+1}(x)] and c[h_{i+2}(x)]
                    h012[0] = getHashFromHash(hash, 0);
                    h012[1] = getHashFromHash(hash, 1);
                    h012[2] = getHashFromHash(hash, 2);
                    // if c[h_{i+1}(x)] become singleton, add their location to stack Q
                    int index3 = h012[mod3(found + 1)];
                    alone[alonePos] = index3;
                    alonePos += ((t2count[index3] >> 2) == 2 ? 1 : 0);
                    t2count[index3] -= 4;
                    t2count[index3] ^= mod3(found + 1);
                    t2hash[index3] ^= hash;
                    // if c[h_{i+2}(x)] become singleton, add their location to stack Q
                    index3 = h012[mod3(found + 2)];
                    alone[alonePos] = index3;
                    alonePos += ((t2count[index3] >> 2) == 2 ? 1 : 0);
                    t2count[index3] -= 4;
                    t2count[index3] ^= mod3(found + 2);
                    t2hash[index3] ^= hash;

                    reverseOrderPos++;
                }
            }
            // the size of P is n, stop repeat
            if (reverseOrderPos == size) {
                break;
            }
            // the size of P is not n, change to another seed
            repeat++;
            Arrays.fill(t2count, (byte) 0);
            Arrays.fill(t2hash, 0);
            Arrays.fill(reverseOrder, 0);
            hashValueMap.clear();
            // if construction doesn't succeed eventually, then there is likely a problem with the hash function
            if (repeat > 100) {
                for (byte[] entry : storage) {
                    Arrays.fill(entry, (byte) 0xFF);
                }
                return;
            }
        }
        // while stack P is not empty do. Here, reverseOrder is the stack P.
        for (int i = reverseOrderPos - 1; i >= 0; i--) {
            // Pop a key x with value V(x), and a location i from the stack P. We have that i ∈ {h_0(x), h_1(x), h_2(x)}.
            // here, the variable found is the location i.
            long hash = reverseOrder[i];
            int found = reverseH[i];
            // Set H[i] ← H[h_0(x)] xor H[h_1(x)] xor H[h_2(x)] xor V(x)
            byte[] value = hashValueMap.get(hash);
            h012[0] = getHashFromHash(hash, 0);
            h012[1] = getHashFromHash(hash, 1);
            h012[2] = getHashFromHash(hash, 2);
            h012[3] = h012[0];
            h012[4] = h012[1];
            Arrays.fill(storage[h012[found]], (byte) 0x00);
            ByteFuseUtils.subi(storage[h012[found]], storage[h012[found + 1]], valueByteLength);
            ByteFuseUtils.subi(storage[h012[found]], storage[h012[found + 2]], valueByteLength);
            ByteFuseUtils.addi(storage[h012[found]], value, valueByteLength);
        }
    }

    /**
     * Computes x mod 3, where 0 <= x < 4.
     *
     * @param x x.
     * @return x mod 3.
     */
    private static byte mod3(int x) {
        if (x > 2) {
            x -= 3;
        }
        return (byte) x;
    }

    @Override
    public byte[][] storage() {
        return storage;
    }

    @Override
    public byte[] decode(T x) {
        int[] positions = positions(x);
        byte[] value = new byte[valueByteLength];
        ByteFuseUtils.addi(value, storage[positions[0]], valueByteLength);
        ByteFuseUtils.addi(value, storage[positions[1]], valueByteLength);
        ByteFuseUtils.addi(value, storage[positions[2]], valueByteLength);

        return value;
    }
}
