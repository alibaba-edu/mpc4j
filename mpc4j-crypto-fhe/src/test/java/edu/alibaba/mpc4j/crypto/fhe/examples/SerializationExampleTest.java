package edu.alibaba.mpc4j.crypto.fhe.examples;

import edu.alibaba.mpc4j.crypto.fhe.*;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.PlainModulus;
import edu.alibaba.mpc4j.crypto.fhe.serialization.ComprModeType;
import edu.alibaba.mpc4j.crypto.fhe.serialization.SealHeader;
import edu.alibaba.mpc4j.crypto.fhe.serialization.SealSerializable;
import edu.alibaba.mpc4j.crypto.fhe.serialization.Serialization;
import org.junit.Test;

import java.io.*;

import static edu.alibaba.mpc4j.crypto.fhe.examples.ExamplesUtils.*;

/**
 * Serialization Example.
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/examples/7_serialization.cpp
 *
 * @author Liqiang Peng
 * @date 2023/12/25
 */
public class SerializationExampleTest {

    @Test
    public void exampleSerialization() throws IOException {
        printExampleBanner("Example: Serialization");
        /*
        We start by briefly discussing the Serializable<T> class template. This is
        a wrapper class that can wrap any serializable class, which include:

            - EncryptionParameters
            - Modulus
            - Plaintext and Ciphertext
            - SecretKey, PublicKey, RelinKeys, and GaloisKeys

        Serializable<T> provides minimal functionality needed to serialize the wrapped
        object by simply forwarding the calls to corresponding functions of the wrapped
        object of type T. The need for Serializable<T> comes from the fact that many
        Microsoft SEAL objects consist of two parts, one of which is pseudorandom data
        independent of the other part. Until the object is actually being used, the
        pseudorandom part can be instead stored as a seed. We will call objects with
        property `seedable'.

        For example, GaloisKeys can often be very large in size, but in reality half
        of the data is pseudorandom and can be stored as a seed. Since GaloisKeys are
        never used by the party that generates them, so it makes sense to expand the
        seed at the point deserialization. On the other hand, we cannot allow the user
        to accidentally try to use an unexpanded GaloisKeys object, which is prevented
        at by ensuring it is always wrapped in a Serializable<GaloisKeys> and can only
        be serialized.

        Only some Microsoft SEAL objects are seedable. Specifically, they are:

            - PublicKey, RelinKeys, and GaloisKeys
            - Ciphertext in secret-key mode (from Encryptor::encrypt_symmetric or
              Encryptor::encrypt_zero_symmetric)

        Importantly, ciphertexts in public-key mode are not seedable. Thus, it may
        be beneficial to use Microsoft SEAL in secret-key mode whenever the public
        key is not truly needed.

        There are a handful of functions that output Serializable<T> objects:

            - Encryptor::encrypt (and variants) output Serializable<Ciphertext>
            - KeyGenerator::create_... output Serializable<T> for different key types

        Note that Encryptor::encrypt is included in the above list, yet it produces
        ciphertexts in public-key mode that are not seedable. This is for the sake of
        consistency in the API for public-key and secret-key encryption. Functions
        that output Serializable<T> objects also have overloads that take a normal
        object of type T as a destination parameter, overwriting it. These overloads
        can be convenient for local testing where no serialization is needed and the
        object needs to be used at the point of construction. Such an object can no
        longer be transformed back to a seeded state.
        */

        /*
        To simulate client-server interaction, we set up a shared C++ stream. In real
        use-cases this can be a network buffer, a filestream, or any shared resource.

        It is critical to note that all data serialized by Microsoft SEAL is in binary
        form, so it is not meaningful to print the data as ASCII characters. Encodings
        such as Base64 would increase the data size, which is already a bottleneck in
        homomorphic encryption. Hence, serialization into text is not supported or
        recommended.

        We feel it is important to remind users that filestream serialization will
        always require the ios::binary flag to signal that the serialized data is
        binary data and not text. For example, an appropriate output filestream could
        be set up as:

            ofstream ofs("filename", ios::binary);

        In this example we use an std::stringstream, where the ios::binary flag is
        not needed. Note that the default constructor of std::stringstream opens the
        stream with ios::in | ios::out so both reading and writing will be possible.
        */
        ByteArrayOutputStream parmsStream = new ByteArrayOutputStream();
        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
        ByteArrayOutputStream skStream = new ByteArrayOutputStream();
        int sizeRlk, sizeRlkBig, sizeEncrypted1, sizeSymEncrypted2, sizeEncryptedProd;
        /*
        The server first determines the computation and sets encryption parameters
        accordingly.
        */
        {
            EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
            int polyModulusDegree = 8192;
            parms.setPolyModulusDegree(polyModulusDegree);
            parms.setCoeffModulus(CoeffModulus.create(polyModulusDegree, new int[]{ 50, 30, 50 }));
            parms.setPlainModulus(PlainModulus.batching(polyModulusDegree, 20));
            SealContext context = new SealContext(parms);

            /*
            Serialization of the encryption parameters to our shared stream is very
            simple with the EncryptionParameters::save function.
            */
            int size = parms.save(parmsStream);

            /*
            The return value of this function is the actual byte count of data written
            to the stream.
            */
            printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
            System.out.print("EncryptionParameters: wrote " + size + " bytes\n");

            /*
            Before moving on, we will take some time to discuss further options in
            serialization. These will become particularly important when the user
            needs to optimize communication and storage sizes.

            It is possible to enable or disable compression for serialization by
            providing EncryptionParameters::save with the desired compression mode as
            in the following examples:

                auto size = parms.save(shared_stream, compr_mode_type::none);
                auto size = parms.save(shared_stream, compr_mode_type::zlib);
                auto size = parms.save(shared_stream, compr_mode_type::zstd);

            If Microsoft SEAL is compiled with Zstandard or ZLIB support, the default
            is to use one of them. If available, Zstandard is preferred over ZLIB due
            to its speed.

            Compression can have a substantial impact on the serialized data size,
            because ciphertext and key data consists of many uniformly random integers
            modulo the coeff_modulus primes. Especially when using CKKS, the primes in
            coeff_modulus can be relatively small compared to the 64-bit words used to
            store the ciphertext and key data internally. Serialization writes full
            64-bit words to the destination buffer or stream, possibly leaving in many
            zero bytes corresponding to the high-order bytes of the 64-bit words. One
            convenient way to get rid of these zeros is to apply a general-purpose
            compression algorithm on the encrypted data. The compression rate can be
            significant (up to 50-60%) when using CKKS with small primes.
            */

            /*
            It is also possible to serialize data directly to a buffer. For this, one
            needs to know an upper bound for the required buffer size, which can be
            obtained using the EncryptionParameters::save_size function. This function
            also accepts the desired compression mode, or uses the default option
            otherwise.

            In more detail, the output of EncryptionParameters::save_size is as follows:

                - Exact buffer size required for compr_mode_type::none;
                - Upper bound on the size required for compr_mode_type::zlib or
                  compr_mode_type::zstd.

            As we can see from the print-out, the sizes returned by these functions
            are significantly larger than the compressed size written into the shared
            stream in the beginning. This is normal: compression yielded a significant
            improvement in the data size, however, it is impossible to know ahead of
            time the exact size of the compressed data. If compression is not used,
            then the size is exactly determined by the encryption parameters.
            */
            printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
            System.out.print(
                "EncryptionParameters: data size upper bound (compr_mode_type::none): "
                    + parms.save(ComprModeType.NONE).length
                    + "\n"
            );
            System.out.print("             "
                + "EncryptionParameters: data size upper bound (compression): "
                + parms.save().length
                + "\n"
            );

            /*
            As an example, we now serialize the encryption parameters to a fixed size
            buffer.
            */
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream(parms.save().length);
            parms.save(byteBuffer);

            /*
            To illustrate deserialization, we load back the encryption parameters
            from our buffer into another instance of EncryptionParameters. Note how
            EncryptionParameters::load in this case requires the size of the buffer,
            which is larger than the actual data size of the compressed parameters.
            The serialization format includes the true size of the data and the size
            of the buffer is only used for a sanity check.
            */
            EncryptionParameters parms2 = new EncryptionParameters();
            parms2.load(context, byteBuffer.toByteArray());

            /*
            We can check that the saved and loaded encryption parameters indeed match.
            */
            printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
            System.out.print("EncryptionParameters: parms == parms2: " + parms.equals(parms2) + "\n");

            /*
            The functions presented and used here exist for all Microsoft SEAL objects
            that are meaningful to serialize. However, it is important to understand
            more advanced techniques that can be used for further compressing the data
            size. We will present these techniques below.
            */
        }

        /*
        Client starts by loading the encryption parameters, sets up the SEALContext,
        and creates the required keys.
        */
        {
            EncryptionParameters parms = new EncryptionParameters();
            parms.load(null, parmsStream.toByteArray());

            SealContext context = new SealContext(parms);

            KeyGenerator keygen = new KeyGenerator(context);
            SecretKey sk = keygen.secretKey();
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            /*
            We need to save the secret key so we can decrypt later.
            */
            sk.save(skStream);

            /*
            As in previous examples, in this example we will encrypt in public-key
            mode. If we want to send a public key over the network, we should instead
            have created it as a seeded object as follows:

                Serializable<PublicKey> pk = keygen.create_public_key();

            In this example we will also use relinearization keys. These we will
            absolutely want to create as seeded objects to minimize communication
            cost, unlike in prior examples.
            */
            SealSerializable<RelinKeys> rlk = keygen.createRelinKeys();

            /*
            To demonstrate the significant space saving from this method, we will
            create another set of relinearization keys, this time fully expanded.
            */
            RelinKeys rlkBig = new RelinKeys();
            keygen.createRelinKeys(rlkBig);

            /*
            We serialize both relinearization keys to demonstrate the concrete size
            difference. If compressed serialization is used, the compression rate
            will be the same in both cases. We omit specifying the compression mode
            to use the default, as determined by the Microsoft SEAL build system.
            */
            sizeRlk = rlk.save(dataStream);
            sizeRlkBig = rlkBig.save(dataStream);

            printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
            System.out.print("Serializable<RelinKeys>: wrote " + sizeRlk + " bytes\n");
            System.out.print("             " + "RelinKeys wrote " + sizeRlkBig + " bytes\n");

            /*
            Next set up the Encryptor, and encrypt some numbers.
            */
            Plaintext plain1 = new Plaintext(new long[]{2}), plain2 = new Plaintext(new long[]{3});
            Encryptor encryptor = new Encryptor(context, pk);

            /*
            The client will not compute on ciphertexts that it creates, so it can
            just as well create Serializable<Ciphertext> objects. In fact, we do
            not even need to name those objects and instead immediately call
            Serializable<Ciphertext>::save.
            */
            sizeEncrypted1 = encryptor.encrypt(plain1).save(dataStream);

            /*
            As we discussed in the beginning of this example, ciphertexts can be
            created in a seeded state in secret-key mode, providing a huge reduction
            in the data size upon serialization. To do this, we need to provide the
            Encryptor with the secret key in its constructor, or at a later point
            with the Encryptor::set_secret_key function, and use the
            Encryptor::encrypt_symmetric function to encrypt.
            */
            encryptor.setSecretKey(sk);
            sizeSymEncrypted2 = encryptor.encryptSymmetric(plain2).save(dataStream);

            /*
            The size reduction is substantial.
            */
            printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
            System.out.print("Serializable<Ciphertext> (public-key): wrote " + sizeEncrypted1 + " bytes\n");
            System.out.print("             "
                + "Serializable<Ciphertext> (seeded secret-key): wrote "
                + sizeSymEncrypted2
                + " bytes\n"
            );

            /*
            We have seen how creating seeded objects can result in huge space
            savings compared to creating unseeded objects. This is particularly
            important when creating Galois keys, which can be very large. We have
            seen how secret-key encryption can be used to achieve much smaller
            ciphertext sizes when the public-key functionality is not needed.

            We would also like to draw attention to the fact there we could easily
            serialize multiple Microsoft SEAL objects sequentially in a stream. Each
            object writes its own size into the stream, so deserialization knows
            exactly how many bytes to read. We will see this working below.
            */
        }

        /*
        The server can now compute on the encrypted data. We will recreate the
        SEALContext and set up an Evaluator here.
        */
        {
            EncryptionParameters parms = new EncryptionParameters();
            parms.load(null, parmsStream.toByteArray());
            SealContext context = new SealContext(parms);

            Evaluator evaluator = new Evaluator(context);

            /*
            Next we need to load relinearization keys and the ciphertexts from our
            data_stream.
            */
            RelinKeys rlk = new RelinKeys();
            Ciphertext encrypted1 = new Ciphertext(), encrypted2 = new Ciphertext();

            /*
            Deserialization is as easy as serialization.
            */
            rlk.load(context, new ByteArrayInputStream(dataStream.toByteArray(), 0, sizeRlk));
            encrypted1.load(
                context,
                new ByteArrayInputStream(dataStream.toByteArray(), sizeRlk + sizeRlkBig, sizeEncrypted1)
            );
            encrypted2.load(
                context,
                new ByteArrayInputStream(
                    dataStream.toByteArray(), sizeRlk + sizeRlkBig + sizeEncrypted1, sizeSymEncrypted2
                )
            );

            /*
            Compute the product and relinearize.
            */
            Ciphertext encryptedProd = new Ciphertext();
            evaluator.multiply(encrypted1, encrypted2, encryptedProd);
            evaluator.relinearizeInplace(encryptedProd, rlk);

            /*
            we use data_stream to communicate encrypted_prod back to the client.
            there is no way to save the encrypted_prod as a seeded object: only
            freshly encrypted secret-key ciphertexts can be seeded. Note how the
            size of the result ciphertext is smaller than the size of a fresh
            ciphertext because it is at a lower level due to the rescale operation.
            */
            sizeEncryptedProd = encryptedProd.save(dataStream);

            printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
            System.out.print("Ciphertext (secret-key): wrote " + sizeEncryptedProd + " bytes\n");
        }

        /*
        In the final step the client decrypts the result.
        */
        {
            EncryptionParameters parms = new EncryptionParameters();
            parms.load(null, parmsStream.toByteArray());
            SealContext context = new SealContext(parms);

            /*
            Load back the secret key from sk_stream.
            */
            SecretKey sk = new SecretKey();
            sk.load(context, skStream.toByteArray());
            Decryptor decryptor = new Decryptor(context, sk);

            Ciphertext encryptedResult = new Ciphertext();
            encryptedResult.load(
                context,
                new ByteArrayInputStream(
                    dataStream.toByteArray(), sizeRlk + sizeRlkBig + sizeEncrypted1 + sizeSymEncrypted2, sizeEncryptedProd
                )
            );

            Plaintext plainResult = new Plaintext();
            decryptor.decrypt(encryptedResult, plainResult);
            long result;
            result = plainResult.getData(0);

            printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
            long true_result = 2 * 3;
            System.out.print("Decrypt the loaded ciphertext\n");
            System.out.print("    + Expected result: " + true_result + "\n");

            System.out.print("    + Computed result: " + result + " ...... Correct.\n");
        }

        /*
        Finally, we give a little bit more explanation of the structure of data
        serialized by Microsoft SEAL. Serialized data always starts with a 16-byte
        SEALHeader struct, as defined in native/src/seal/serialization.h, and is
        followed by the possibly compressed data for the object.

        A SEALHeader contains the following data:

            [offset 0] 2-byte magic number 0xA15E (Serialization::seal_magic)
            [offset 2] 1-byte indicating the header size in bytes (always 16)
            [offset 3] 1-byte indicating the Microsoft SEAL major version number
            [offset 4] 1-byte indicating the Microsoft SEAL minor version number
            [offset 5] 1-byte indicating the compression mode type
            [offset 6] 2-byte reserved field (unused)
            [offset 8] 8-byte size in bytes of the serialized data, including the header

        Currently Microsoft SEAL supports only little-endian systems.

        As an example, we demonstrate the SEALHeader created by saving a plaintext.
        Note that the SEALHeader is never compressed, so there is no need to specify
        the compression mode.
        */
        Plaintext pt = new Plaintext("1x^2 + 3");
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        int data_size = pt.save(stream);

        /*
        We can now load just the SEALHeader back from the stream as follows.
        */
        SealHeader header = new SealHeader();
        Serialization.loadHeader(new ByteArrayInputStream(stream.toByteArray()), header);

        /*
        Now confirm that the size of data written to stream matches with what is
        indicated by the SEALHeader.
        */
        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        System.out.print("Size written to stream: " + data_size + " bytes\n");
        System.out.print("             " + "Size indicated in SEALHeader: " + header.size + " bytes\n");
        System.out.print("\n");
    }
}
