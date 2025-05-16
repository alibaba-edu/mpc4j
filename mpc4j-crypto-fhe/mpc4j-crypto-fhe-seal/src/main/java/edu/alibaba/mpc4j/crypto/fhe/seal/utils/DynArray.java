package edu.alibaba.mpc4j.crypto.fhe.seal.utils;

import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.seal.serialization.SealCloneable;
import edu.alibaba.mpc4j.crypto.fhe.seal.serialization.SealVersion;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.MultilineRecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.io.*;

/**
 * The DynArray class is mainly intended for internal use and provides the underlying data structure for Plaintext and
 * Ciphertext classes.
 * <p>
 * The implementation is from
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/dynarray.h">dynarray.h</a>.
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/31
 */
public class DynArray extends AbstractDynArray implements SealCloneable {
    /**
     * Creates a new DynArray. No memory is allocated by this constructor.
     */
    public DynArray() {
        super();
    }

    /**
     * Creates a new DynArray with given size.
     *
     * @param size the size of the array.
     */
    public DynArray(int size) {
        super(size);
    }

    /**
     * Creates a new DynArray with given capacity and size.
     *
     * @param capacity the capacity of the array.
     * @param size     the size of the array.
     */
    public DynArray(int capacity, int size) {
        super(capacity, size);
    }

    /**
     * Creates a new DynArray with given capacity, initialized with data from
     * a given buffer.
     *
     * @param data     desired contents of the array.
     * @param capacity the capacity of the array.
     */
    public DynArray(long[] data, int capacity) {
        super(data, capacity);
    }

    /**
     * Creates a new DynArray initialized with data from a given buffer.
     *
     * @param data desired contents of the array.
     */
    public DynArray(long[] data) {
        super(data);
    }

    /**
     * Creates a new DynArray with given size wrapping a given pointer. This
     * constructor allocates no memory. If the DynArray goes out of scope, the
     * Pointer object given here is destroyed. On resizing the DynArray to larger
     * size, the data will be copied over to a new allocation from the memory pool
     * pointer to by the given MemoryPoolHandle and the Pointer object given here
     * will subsequently be destroyed. Unlike the other constructors, this one
     * exposes the option of not automatically zero-filling the allocated memory.
     *
     * @param data     desired contents of the array.
     * @param capacity the capacity of the array.
     * @param size     the size of the array.
     * @param fillZero if true, fills data with zeros.
     */
    public DynArray(long[] data, int capacity, int size, boolean fillZero) {
        super(data, capacity, size, fillZero);
    }

    /**
     * Creates a new DynArray by copying a given one.
     *
     * @param copy the DynArray to copy from.
     */
    public DynArray(DynArray copy) {
        super(copy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DynArray that)) {
            return false;
        }
        if (this.size != that.size) {
            return false;
        }
        // data[0, size) is equal
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        for (int i = 0; i < size; i++) {
            equalsBuilder.append(this.data[i], that.data[i]);
        }
        return equalsBuilder.isEquals();
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this, new MultilineRecursiveToStringStyle()).toString();
    }

    @Override
    public void saveMembers(OutputStream outputStream) throws IOException {
        LittleEndianDataOutputStream stream = new LittleEndianDataOutputStream(outputStream);
        stream.writeLong(size);
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                stream.writeLong(data[i]);
            }
        }
        stream.close();
    }

    @Override
    public void loadMembers(SealContext context, InputStream inputStream, SealVersion version) throws IOException {
        LittleEndianDataInputStream stream = new LittleEndianDataInputStream(inputStream);
        int readSize = (int) stream.readLong();
        // Set new size; this is potentially unsafe if size64 was not checked against expected_size
        resize(readSize);
        // Read data
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                data[i] = stream.readLong();
            }
        }
        stream.close();
    }

    @Override
    public int load(SealContext context, InputStream inputStream) throws IOException {
        // there is no valid check in DynArray
        return unsafeLoad(context, inputStream);
    }

    @Override
    public void load(SealContext context, byte[] in) throws IOException {
        // there is no valid chekc in DynArray
        unsafeLoad(context, in);
    }
}
