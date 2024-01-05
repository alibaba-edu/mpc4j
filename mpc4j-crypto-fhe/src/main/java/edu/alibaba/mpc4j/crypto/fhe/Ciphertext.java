package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext.ContextData;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.ParmsId;
import edu.alibaba.mpc4j.crypto.fhe.rand.UniformRandomGenerator;
import edu.alibaba.mpc4j.crypto.fhe.rand.UniformRandomGeneratorInfo;
import edu.alibaba.mpc4j.crypto.fhe.serialization.ComprModeType;
import edu.alibaba.mpc4j.crypto.fhe.serialization.SealCloneable;
import edu.alibaba.mpc4j.crypto.fhe.serialization.SealVersion;
import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;
import edu.alibaba.mpc4j.crypto.fhe.utils.DynArray;
import edu.alibaba.mpc4j.crypto.fhe.utils.RingLwe;
import edu.alibaba.mpc4j.crypto.fhe.utils.ValCheck;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.*;
import java.util.Arrays;

/**
 * Class to store a ciphertext element. The data for a ciphertext consists
 * of two or more polynomials, which are in Microsoft SEAL stored in a CRT
 * form with respect to the factors of the coefficient modulus. This data
 * itself is not meant to be modified directly by the user, but is instead
 * operated on by functions in the Evaluator class. The size of the backing
 * array of a ciphertext depends on the encryption parameters and the size
 * of the ciphertext (at least 2). If the size of the ciphertext is T,
 * the poly_modulus_degree encryption parameter is N, and the number of
 * primes in the coeff_modulus encryption parameter is K, then the
 * ciphertext backing array requires precisely 8*N*K*T bytes of memory.
 * A ciphertext also carries with it the parms_id of its associated
 * encryption parameters, which is used to check the validity of the
 * ciphertext for homomorphic operations and decryption.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/ciphertext.h
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/13
 */
public class Ciphertext implements SealCloneable {
    /**
     * parms_id
     */
    private ParmsId parmsId = ParmsId.parmsIdZero();
    /**
     * whether the ciphertext is in NTT form.
     */
    private boolean isNttForm = false;
    /**
     * the size of the ciphertext
     */
    private int size = 0;
    /**
     * the degree of the polynomial
     */
    private int polyModulusDegree = 0;
    /**
     * the number of primes in the coefficient modulus
     */
    private int coeffModulusSize = 0;
    /**
     * scale, only needed when using the CKKS encryption scheme
     */
    private double scale = 1.0;
    /**
     * correction factor, only needed when using the BGV encryption scheme
     */
    private long correctionFactor = 1;
    /**
     * ciphertext data
     */
    private final DynArray data = new DynArray();

    /**
     * Constructs an empty ciphertext allocating no memory.
     */
    public Ciphertext() {
    }

    /**
     * Constructs an empty ciphertext with capacity 2. In addition to the
     * capacity, the allocation size is determined by the highest-level
     * parameters associated to the given SEALContext.
     *
     * @param context the SEALContext.
     */
    public Ciphertext(SealContext context) {
        reserve(context, 2);
    }

    /**
     * Constructs an empty ciphertext with capacity 2. In addition to the
     * capacity, the allocation size is determined by the encryption parameters
     * with given parms_id.
     *
     * @param context the context.
     * @param parmsId the parms_id corresponding to the encryption parameters to be used.
     */
    public Ciphertext(SealContext context, ParmsId parmsId) {
        reserve(context, parmsId, 2);
    }

    /**
     * Constructs an empty ciphertext with given capacity. In addition to
     * the capacity, the allocation size is determined by the given
     * encryption parameters.
     *
     * @param context      the context.
     * @param parmsId      the parms_id corresponding to the encryption parameters to be used.
     * @param sizeCapacity the capacity.
     */
    public Ciphertext(SealContext context, ParmsId parmsId, int sizeCapacity) {
        reserve(context, parmsId, sizeCapacity);
    }

    /**
     * Copies a given ciphertext to the current one.
     *
     * @param copy the ciphertext to copy from.
     */
    public void copyFrom(Ciphertext copy) {
        if (this == copy) {
            return;
        }
        // copy over fields, but do not need to copy parms_id
        this.parmsId = copy.parmsId;
        this.isNttForm = copy.isNttForm();
        this.scale = copy.scale;
        this.correctionFactor = copy.correctionFactor;
        // Then resize
        resizeInternal(copy.size, copy.polyModulusDegree, copy.coeffModulusSize);
        // copy data
        System.arraycopy(copy.data.data(), 0, data.data(), 0, size * polyModulusDegree * coeffModulusSize);
    }

    /**
     * Allocates enough memory to accommodate the backing array of a ciphertext
     * with given capacity. In addition to the capacity, the allocation size is
     * determined by the encryption parameters corresponding to the given
     * parms_id.
     *
     * @param context      the SEALContext.
     * @param parmsId      the parms_id corresponding to the encryption parameters to be used.
     * @param sizeCapacity the capacity.
     */
    public void reserve(SealContext context, ParmsId parmsId, int sizeCapacity) {
        if (!context.isParametersSet()) {
            throw new IllegalArgumentException("encryption parameters are not set correctly");
        }
        SealContext.ContextData contextData = context.getContextData(parmsId);
        if (contextData == null) {
            throw new IllegalArgumentException("parms_id is not valid for encryption parameters");
        }
        EncryptionParameters parms = contextData.parms();
        this.parmsId = contextData.parmsId();
        reserveInternal(sizeCapacity, parms.polyModulusDegree(), parms.coeffModulus().length);
    }

    /**
     * Allocates enough memory to accommodate the backing array of a ciphertext
     * with given capacity. In addition to the capacity, the allocation size is
     * determined by the highest-level parameters associated to the given
     * SEALContext.
     *
     * @param context      the SEALContext.
     * @param sizeCapacity the capacity.
     */
    public void reserve(SealContext context, int sizeCapacity) {
        reserve(context, context.firstParmsId(), sizeCapacity);
    }

    /**
     * Allocates enough memory to accommodate the backing array of a ciphertext
     * with given capacity. In addition to the capacity, the allocation size is
     * determined by the current encryption parameters.
     *
     * @param sizeCapacity the capacity.
     */
    public void reserve(int sizeCapacity) {
        reserveInternal(sizeCapacity, polyModulusDegree, coeffModulusSize);
    }

    private void reserveInternal(int sizeCapacity, int polyModulusDegree, int coeffModulusSize) {
        if ((sizeCapacity < Constants.SEAL_CIPHERTEXT_SIZE_MIN && sizeCapacity != 0) || sizeCapacity > Constants.SEAL_CIPHERTEXT_SIZE_MAX) {
            throw new IllegalArgumentException("invalid size capacity");
        }
        // sizeCapacity * polyModulusDegree * coeffModulusSize is the number of long value needed by the ciphertext
        int newDataCapacity = Common.mulSafe(sizeCapacity, polyModulusDegree, false, coeffModulusSize);
        int newDataSize = Math.min(newDataCapacity, data.size());

        // First reserve, then resize
        data.reserve(newDataCapacity);
        data.resize(newDataSize);

        // Set the size
        size = Math.min(sizeCapacity, size);
        this.polyModulusDegree = polyModulusDegree;
        this.coeffModulusSize = coeffModulusSize;
    }

    /**
     * Resizes the ciphertext to given size, reallocating if the capacity
     * of the ciphertext is too small. The ciphertext parameters are
     * determined by the given SEALContext and parms_id.
     * <p>
     * This function is mainly intended for internal use and is called
     * automatically by functions such as Evaluator::multiply and
     * Evaluator::relinearize. A normal user should never have a reason
     * to manually resize a ciphertext.
     *
     * @param context the SEALContext.
     * @param parmsId the parms_id corresponding to the encryption parameters to be used.
     * @param size    the new size.
     */
    public void resize(SealContext context, ParmsId parmsId, int size) {
        if (!context.isParametersSet()) {
            throw new IllegalArgumentException("encryption parameters are not set correctly");
        }
        SealContext.ContextData contextData = context.getContextData(parmsId);
        if (contextData == null) {
            throw new IllegalArgumentException("parms_id is not valid for encryption parameters");
        }
        EncryptionParameters parms = contextData.parms();
        this.parmsId = parmsId;
        resizeInternal(size, parms.polyModulusDegree(), parms.coeffModulus().length);
    }

    /**
     * Resizes the ciphertext to given size, reallocating if the capacity
     * of the ciphertext is too small. The ciphertext parameters are
     * determined by the highest-level parameters associated to the given
     * SEALContext.
     * <p>
     * This function is mainly intended for internal use and is called
     * automatically by functions such as Evaluator::multiply and
     * Evaluator::relinearize. A normal user should never have a reason
     * to manually resize a ciphertext.
     *
     * @param context the SEALContext.
     * @param size    the new size.
     */
    public void resize(SealContext context, int size) {
        resize(context, context.firstParmsId(), size);
    }

    /**
     * Resizes the ciphertext to given size, reallocating if the capacity
     * of the ciphertext is too small.
     * <p>
     * This function is mainly intended for internal use and is called
     * automatically by functions such as Evaluator::multiply and
     * Evaluator::relinearize. A normal user should never have a reason
     * to manually resize a ciphertext.
     *
     * @param size the new size.
     */
    public void resize(int size) {
        // Note: poly_modulus_degree_ and coeff_modulus_size_ are either valid
        // or coeff_modulus_size_ is zero (in which case no memory is allocated).
        resizeInternal(size, polyModulusDegree, coeffModulusSize);
    }

    private void resizeInternal(int size, int polyModulusDegree, int coeffModulusSize) {
        if ((size < Constants.SEAL_CIPHERTEXT_SIZE_MIN && size != 0) || size > Constants.SEAL_CIPHERTEXT_SIZE_MAX) {
            throw new IllegalArgumentException("invalid size");
        }

        // Resize the data
        int newDataSize = Common.mulSafe(size, polyModulusDegree, false, coeffModulusSize);
        data.resize(newDataSize);

        // Set the size parameters
        this.size = size;
        this.polyModulusDegree = polyModulusDegree;
        this.coeffModulusSize = coeffModulusSize;
    }

    private void expandSeed(SealContext context, UniformRandomGeneratorInfo prngInfo) {
        ContextData contextData = context.getContextData(parmsId);
        UniformRandomGenerator prng = prngInfo.makePrng();
        if (prng == null) {
            throw new IllegalArgumentException("unsupported prng_type");
        }
        RingLwe.samplePolyUniform(prng, contextData.parms(), data.data(), getPolyOffset(1));
    }

    /**
     * Resets the ciphertext. This function releases any memory allocated
     * by the ciphertext. It also sets all encryption parameter specific
     * size information to zero.
     */
    public void release() {
        parmsId = ParmsId.parmsIdZero();
        isNttForm = false;
        size = 0;
        polyModulusDegree = 0;
        coeffModulusSize = 0;
        scale = 1.0;
        correctionFactor = 1;
        data.release();
    }

    /**
     * Returns a reference to the backing DynArray object.
     *
     * @return a reference to the backing DynArray object.
     */
    public DynArray dynArray() {
        return data;
    }

    /**
     * Returns the ciphertext data.
     *
     * @return the ciphertext data.
     */
    public long[] data() {
        return data.data();
    }

    /**
     * Returns the index of a particular polynomial in the ciphertext data.
     * Note that Microsoft SEAL stores each polynomial in the ciphertext
     * modulo all the K primes in the coefficient modulus.The index returned
     * by this function is the beginning index (constant coefficient) of the
     * first one of these K polynomials.
     *
     * @param polyIndex the index of the polynomial in the ciphertext.
     * @return the beginning index of the particular polynomial.
     */
    public int getPolyOffset(int polyIndex) {
        assert polyIndex >= 0 && polyIndex < size;
        int polyUint64Count = Common.mulSafe(polyModulusDegree, coeffModulusSize, false);
        return Common.mulSafe(polyIndex, polyUint64Count, false);
    }

    /**
     * Returns the actual value to a polynomial coefficient at a particular
     * index in the ciphertext data. If the polynomial modulus has degree N,
     * and the number of primes in the coefficient modulus is K, then the
     * ciphertext contains size*N*K coefficients. Thus, the coeff_index
     * has a range of [0, size*N*K).
     *
     * @param coeffIndex the index of the coefficient.
     * @return the coefficient.
     */
    public long getCoeff(int coeffIndex) {
        return data.at(coeffIndex);
    }

    /**
     * Returns the number of primes in the coefficient modulus of the
     * associated encryption parameters. This directly affects the
     * allocation size of the ciphertext.
     *
     * @return the number of primes in the coefficient modulus.
     */
    public int getCoeffModulusSize() {
        return coeffModulusSize;
    }

    /**
     * Returns the degree of the polynomial modulus of the associated
     * encryption parameters. This directly affects the allocation size
     * of the ciphertext.
     *
     * @return the degree of the polynomial.
     */
    public int polyModulusDegree() {
        return polyModulusDegree;
    }

    /**
     * Returns the size of the ciphertext.
     *
     * @return the size of the ciphertext.
     */
    public int size() {
        return size;
    }

    /**
     * Returns the capacity of the allocation. This means the largest size
     * of the ciphertext that can be stored in the current allocation with
     * the current encryption parameters.
     *
     * @return the capacity of the allocation.
     */
    public int getSizeCapacity() {
        int polyUint64Count = polyModulusDegree * coeffModulusSize;
        return polyUint64Count > 0 ? data.capacity() / polyUint64Count : 0;
    }

    /**
     * Check whether the current ciphertext is transparent, i.e. does not require
     * a secret key to decrypt. In typical security models such transparent
     * ciphertexts would not be considered to be valid. Starting from the second
     * polynomial in the current ciphertext, this function returns true if all
     * following coefficients are identically zero. Otherwise, returns false.
     *
     * @return true if the ciphertext is transparent, otherwise false.
     */
    public boolean isTransparent() {
        boolean b1 = data.size() == 0 || (size < Constants.SEAL_CIPHERTEXT_SIZE_MIN);
        // check if all values in the remaining polynomials is all 0.
        boolean b2 = true;
        int startIndex = getPolyOffset(1);
        for (int i = startIndex; i < data.data().length; i++) {
            if (data.data()[i] != 0) {
                b2 = false;
                break;
            }
        }
        return b1 || b2;
    }

    /**
     * Returns a reference to parms_id.
     *
     * @return a reference to parms_id.
     */
    public ParmsId parmsId() {
        return parmsId;
    }

    /**
     * Sets parms_id to the given one.
     *
     * @param parmsId the given parms_id.
     */
    public void setParmsId(ParmsId parmsId) {
        this.parmsId = parmsId;
    }

    /**
     * Returns a reference to the scale. This is only needed when using the CKKS
     * encryption scheme. The user should have little or no reason to ever change
     * the scale by hand.
     *
     * @return the scale.
     */
    public double scale() {
        return scale;
    }

    /**
     * Sets the scale.
     *
     * @param scale the scale.
     */
    public void setScale(double scale) {
        this.scale = scale;
    }

    /**
     * Returns a reference to the correction factor. This is only needed when using
     * the BGV encryption scheme. The user should have little or no reason to ever
     * change the correction factor by hand.
     *
     * @return the correction factor.
     */
    public long correctionFactor() {
        return correctionFactor;
    }

    /**
     * Sets the correction factor.
     *
     * @param correctionFactor correction factor.
     */
    public void setCorrectionFactor(long correctionFactor) {
        this.correctionFactor = correctionFactor;
    }

    /**
     * Returns whether the ciphertext is in NTT form.
     *
     * @return true if the ciphertext is in NTT form; false otherwise.
     */
    public boolean isNttForm() {
        return isNttForm;
    }

    /**
     * Sets the NTT form of the ciphertext.
     *
     * @param isNttForm the NTT form.
     */
    public void setIsNttForm(boolean isNttForm) {
        this.isNttForm = isNttForm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ciphertext)) {
            return false;
        }
        Ciphertext that = (Ciphertext) o;
        return new EqualsBuilder()
            .append(this.isNttForm, that.isNttForm)
            .append(this.size, that.size)
            .append(this.polyModulusDegree, that.polyModulusDegree)
            .append(this.coeffModulusSize, that.coeffModulusSize)
            .append(this.scale, that.scale)
            .append(this.correctionFactor, that.correctionFactor)
            .append(this.parmsId, that.parmsId)
            .append(this.data, that.data)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(parmsId)
            .append(isNttForm)
            .append(size)
            .append(polyModulusDegree)
            .append(coeffModulusSize)
            .append(scale)
            .append(correctionFactor)
            .append(data)
            .toHashCode();
    }

    private boolean hasSeedMarker() {
        return data.size() > 0 && (size == 2)
            && data.data()[getPolyOffset(1)] == UniformRandomGeneratorInfo.PRNG_INFO_INDICATOR;
    }

    @Override
    public void saveMembers(OutputStream outputStream) throws IOException {
        DataOutputStream stream = new DataOutputStream(outputStream);
        parmsId.saveMembers(stream);
        stream.writeByte(isNttForm ? 0x01 : 0x00);
        stream.writeLong(size);
        stream.writeLong(polyModulusDegree);
        stream.writeLong(coeffModulusSize);
        stream.writeDouble(scale);
        stream.writeLong(correctionFactor);

        if (hasSeedMarker()) {
            UniformRandomGeneratorInfo info = new UniformRandomGeneratorInfo();
            info.load(data.data(), getPolyOffset(1) + 1);

            int dataSize = data.size();
            int halfSize = dataSize / 2;
            // Save_members must be a const method.
            // Create an alias of data_; must be handled with care.
            DynArray aliasData = new DynArray(Arrays.copyOf(data.data(), halfSize));
            aliasData.save(outputStream, ComprModeType.NONE);

            // Save the UniformRandomGeneratorInfo
            info.save(outputStream, ComprModeType.NONE);
        } else {
            // Save the DynArray
            data.save(outputStream, ComprModeType.NONE);
        }
        stream.close();
    }

    @Override
    public void loadMembers(SealContext context, InputStream inputStream, SealVersion version) throws IOException {
        // Verify parameters
        if (!context.isParametersSet()) {
            throw new IllegalArgumentException("encryption parameters are not set correctly");
        }
        DataInputStream stream = new DataInputStream(inputStream);
        parmsId.loadMembers(stream);
        byte byteIsNttForm = stream.readByte();
        isNttForm = byteIsNttForm != 0x00;
        size = (int) stream.readLong();
        polyModulusDegree = (int) stream.readLong();
        coeffModulusSize = (int) stream.readLong();
        scale = stream.readDouble();
        correctionFactor = stream.readLong();

        // Checking the validity of loaded metadata
        // Note: We allow pure key levels here! This is to allow load_members
        // to be used also when loading derived objects like PublicKey. This
        // further means that functions reading in Ciphertext objects must check
        // that for those use-cases the Ciphertext truly is at the data level
        // if it is supposed to be. In other words, one cannot assume simply
        // based on load_members succeeding that the Ciphertext is valid for
        // computations.
        if (!ValCheck.isMetaDataValidFor(this, context, true)) {
            throw new IllegalArgumentException("ciphertext data is invalid");
        }

        // Compute the total uint64 count required and reserve memory.
        // Note that this must be done after the metadata is checked for validity.
        int totalUint64Count = Common.mulSafe(size, polyModulusDegree, false, coeffModulusSize);

        // Reserve memory for the entire (expected) ciphertext data
        data.reserve(totalUint64Count);

        // Load the data. Note that we are supplying also the expected maximum
        // size of the loaded DynArray. This is an important security measure to
        // prevent a malformed DynArray from causing arbitrarily large memory
        // allocations.
        data.load(context, stream);

        // Expected buffer size in the seeded case
        int seededUint64Count = polyModulusDegree * coeffModulusSize;

        // This is the case where we need to expand a seed, otherwise full
        // ciphertext data was already (possibly) loaded and we are done
        if (data.size() == seededUint64Count) {
            // Single polynomial size data was loaded, so we are in the seeded
            // ciphertext case. Next load the UniformRandomGeneratorInfo.
            UniformRandomGeneratorInfo prngInfo = new UniformRandomGeneratorInfo();
            prngInfo.load(context, inputStream);

            // Set up a UniformRandomGenerator and expand
            data.resize(totalUint64Count);
            expandSeed(context, prngInfo);
        }

        // Verify that the buffer is correct
        if (!ValCheck.isBufferValid(this)) {
            throw new IllegalArgumentException("ciphertext data is invalid");
        }
        stream.close();
    }

    @Override
    public int load(SealContext context, InputStream inputStream) throws IOException {
        int inSize = unsafeLoad(context, inputStream);
        if (!ValCheck.isValidFor(this, context)) {
            throw new IllegalArgumentException("ciphertext data is invalid");
        }
        return inSize;
    }

    @Override
    public void load(SealContext context, byte[] in) throws IOException {
        unsafeLoad(context, in);
        if (!ValCheck.isValidFor(this, context)) {
            throw new IllegalArgumentException("ciphertext data is invalid");
        }
    }
}
