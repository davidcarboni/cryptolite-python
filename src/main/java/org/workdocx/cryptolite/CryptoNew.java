/**
 * Copyright (C) 2011 WorkDocx Ltd.
 */
package org.workdocx.cryptolite;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

/**
 * 
 * This class provides simple encryption and decryption of Strings and streams.
 * <p>
 * This class uses the {@value #CIPHER_ALGORITHM} algorithm in {@value #CIPHER_MODE} cipher mode,
 * padding and initialisation vector handling. This hides the complexity involved in selecting types
 * and values for these and allows the caller to simply request encryption and decryption
 * operations.
 * <p>
 * Some effort has been invested in choosing these values so that they are suitable for the needs of
 * WorkDocx:
 * <ul>
 * <li>AES cipher: NIST standard for the transmission of classified US government data.</li>
 * <li>CTR cipher mode: NIST standard cipher mode.</li>
 * <li>No padding: the CTR cipher mode is a "streaming" mode and therefore does not require padding.
 * </li>
 * <li>Inline initialisation vector: this avoids the need to handle the IV as an additional
 * out-of-band parameter.</li>
 * </ul>
 * <p>
 * Notes on background information used in selectingthe cipher, mode and padding:
 * <p>
 * <ul>
 * <li>Wikipedia: http://en.wikipedia.org/wiki/Advanced_Encryption_Standard</li>
 * </ul>
 * "AES was announced by National Institute of Standards and Technology (NIST) as U.S. FIPS PUB 197
 * (FIPS 197) on November 26, 2001 after a 5-year standardization process in which fifteen competing
 * designs were presented and evaluated before Rijndael was selected as the most suitable (see
 * Advanced Encryption Standard process for more details). It became effective as a Federal
 * government standard on May 26, 2002 after approval by the Secretary of Commerce. It is available
 * in many different encryption packages. AES is the first publicly accessible and open cipher
 * approved by the NSA for top secret information."
 * <p>
 * <ul>
 * <li>Beginning Cryptography with Java</li>
 * </ul>
 * "CTR has been standardised by NIST in SP 800-38a and RFC 3686"
 * <p>
 * <ul>
 * <li>http://www.daemonology.net/blog/2009-06-11-cryptographic-right-answers.html</li>
 * </ul>
 * "AES is about as standard as you can get, and has done a good job of resisting cryptologic
 * attacks over the past decade. Using CTR mode avoids the weakness of ECB mode, the complex (and
 * bug-prone) process of padding and unpadding of partial blocks (or ciphertext stealing), and
 * vastly reduces the risk of side channel attacks thanks to the fact that the data being input to
 * AES is not sensitive."
 * <p>
 * NOTE: CTR mode is "malleable", so if there is a requirement to assure the integrity of the data,
 * on top of encrypting it, this blog recommends adding an HMAC (Hash-based Message Authentication
 * Code).
 * <p>
 * <ul>
 * <li>http://www.javamex.com/tutorials/cryptography/initialisation_vector.shtml</li>
 * </ul>
 * The initialisation vector used in this class is a random one which, according to this site,
 * provides about the same risk of collision as OFB. Given that a relatively small number of items
 * will be encrypted, as compared to a stream of messages which may contain tens of thousands of
 * messages, this makes is a good choice.
 * <p>
 * <ul>
 * <li>Wikipedia: http://en.wikipedia.org/wiki/Advanced_Encryption_Standard</li>
 * </ul>
 * U.S. Government announced ... "The design and strength of all key lengths of the AES algorithm
 * (i.e., 128, 192 and 256) are sufficient to protect classified information up to the SECRET level.
 * TOP SECRET information will require use of either the 192 or 256 key lengths". This class has
 * been designed to use 128-bit keys as this does not require unlimited strength encryption and
 * still provides a level of protection equivalent to that used by SECRET level information. Since
 * we are not transmitting these data over the Internet, this seems a reasonable level of
 * protection. It is not clear at the time of writing what the performance impact of using longer
 * keys will be in practice, so this is not a factor in selection of the key size.
 * <p>
 * 
 * @author David Carboni
 * 
 */
public class CryptoNew {

	/** The name of the cipher algorithm to use for symmetric cryptographic operations. */
	public static final String CIPHER_ALGORITHM = "AES";
	/** The name of the cipher mode to use for symmetric cryptographic operations. */
	public static final String CIPHER_MODE = "CTR";
	/** The name of the padding type to use for symmetric cryptographic operations. */
	public static final String CIPHER_PADDING = "NoPadding";

	/**
	 * The full name of the {@link Cipher} to use for cryptographic operations, in a format suitable
	 * for passing to the JCE.
	 */
	public static final String CIPHER_NAME = CIPHER_ALGORITHM + "/" + CIPHER_MODE + "/" + CIPHER_PADDING;

	/** The {@link Cipher} for this instance. */
	private final Cipher cipher;

	/**
	 * Initialises the instance by getting and caching a {@link Cipher} instance for
	 * {@value #CIPHER_NAME}.
	 */
	public CryptoNew() {

		try {

			// Get a Cipher instance:
			cipher = Cipher.getInstance(CIPHER_NAME, SecurityProvider.getProviderName());

		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Unable to locate algorithm for " + CIPHER_NAME, e);
		} catch (NoSuchProviderException e) {
			throw new RuntimeException("Unable to locate provider. Are the BouncyCastle libraries installed?", e);
		} catch (NoSuchPaddingException e) {
			throw new RuntimeException("Unable to locate padding method " + CIPHER_PADDING, e);
		}
	}

	/**
	 * This method encrypts the given String, returning a base-64 encoded String. Note that the
	 * base-64 String will be longer than the input String by 30-40% for an 85 character String. An
	 * 85-character database field can therefore only hold 60 characters of (single-byte character)
	 * plaintext.
	 * 
	 * @param string
	 *            The input String.
	 * @param key
	 *            The key to be used to encrypt the String.
	 * @return The encrypted String, base-64 encoded, or null if the given String is null.
	 * @throws InvalidKeyException
	 *             If the given key is not a valid {@value #CIPHER_ALGORITHM} key.
	 */
	public String encrypt(String string, SecretKey key) throws InvalidKeyException {

		// Basic null check. 
		// An empty string can be encrypted:
		if (string == null) {
			return null;
		}

		// Convert the input Sting to a byte array:
		byte[] bytes = Codec.toByteArray(string);

		// Get the initialisation vector:
		byte[] iv = generateInitialisationVector();

		// Prepare a cipher instance:
		initCipher(Cipher.ENCRYPT_MODE, key, iv);

		// Encrypt the data:
		byte[] result;
		try {
			result = cipher.doFinal(bytes);
		} catch (IllegalBlockSizeException e) {
			throw new RuntimeException("Block-size exception when completing String encrypiton.", e);
		} catch (BadPaddingException e) {
			throw new RuntimeException("Padding error detected when completing String encrypiton.", e);
		}

		// Concatenate the iv and the encrypted data, removing any unused bytes at the end of the encrypted array:
		result = ArrayUtils.addAll(iv, result);

		return Codec.toBase64String(result);
	}

	/**
	 * This method decrypts the given String and returns the plain text.
	 * 
	 * @param encrypted
	 *            The encrypted String, base-64 encoded, as returned by
	 *            {@link #encrypt(String, SecretKey)}.
	 * @param key
	 *            The key to be used for decryption.
	 * @return The decrypted String, or null if the encrypted String is null.
	 * @throws InvalidKeyException
	 *             If the given key is not a valid {@value #CIPHER_ALGORITHM} key.
	 */
	public String decrypt(String encrypted, SecretKey key) throws InvalidKeyException {

		// Basic null/empty check.
		// An empty string can be encrypted, but not decrypted:
		if (StringUtils.isEmpty(encrypted)) {
			return encrypted;
		}

		// Separate the IV and the data:
		byte[] bytes = Codec.fromBase64String(encrypted);
		int ivSize = cipher.getBlockSize();
		byte[] iv = ArrayUtils.subarray(bytes, 0, ivSize);
		byte[] data = ArrayUtils.subarray(bytes, ivSize, bytes.length);

		// Prepare a cipher instance with the IV:
		initCipher(Cipher.DECRYPT_MODE, key, iv);

		// Decrypt the data:
		byte[] result;
		try {
			result = cipher.doFinal(data);
		} catch (IllegalBlockSizeException e) {
			throw new RuntimeException("Block-size exception when completing String encrypiton.", e);
		} catch (BadPaddingException e) {
			throw new RuntimeException("Padding error detected when completing String encrypiton.", e);
		}

		// Remove any unused bytes at the end of the output array:
		return Codec.fromByteArray(result);
	}

	/**
	 * This method wraps the destination {@link OutputStream} with a {@link CipherOutputStream}.
	 * <p>
	 * Typical usage is when you have an InputStream for a source of unencrypted data, such as a
	 * user-uploaded file, and an OutputStream to write the input to disk. You would call this
	 * method to wrap the OutputStream and use the returned {@link CipherOutputStream} instead to
	 * write the data to, so that it is encrypted as it is written to disk.
	 * <p>
	 * Note that this method writes an initialisation vector to the destination OutputStream, so the
	 * destination parameter will have some bytes written to it before this method returns. These
	 * bytes are necessary for decryption and a corresponding call to
	 * {@link #decrypt(InputStream, SecretKey)} will read and filter them out from the underlying
	 * InputStream before returning it.
	 * 
	 * @param destination
	 *            The output stream to be wrapped with a {@link CipherOutputStream}.
	 * @param key
	 *            The key to be used to encrypt data written to the returned
	 *            {@link CipherOutputStream}.
	 * @return A {@link CipherOutputStream}, which wraps the given {@link OutputStream}.
	 * @throws IOException
	 *             If an error occurs in writing the initialisation vector to the destination
	 *             stream.
	 * @throws InvalidKeyException
	 *             If the given key is not a valid {@value #CIPHER_ALGORITHM} key.
	 */
	public OutputStream encrypt(OutputStream destination, SecretKey key) throws IOException, InvalidKeyException {

		// Basic null check. 
		// An empty stream can be encrypted:
		if (destination == null) {
			return null;
		}

		// Initialise the CipherOutputStream with the initialisation vector:
		byte[] iv = generateInitialisationVector();
		destination.write(iv);

		// Get a cipher instance and instantiate the CipherOutputStream:
		initCipher(Cipher.ENCRYPT_MODE, key, iv);
		CipherOutputStream cipherOutputStream = new CipherOutputStream(destination, cipher);

		// Return the initialised stream:
		return cipherOutputStream;
	}

	/**
	 * This method wraps the source {@link InputStream} with a {@link CipherInputStream}.
	 * <p>
	 * Typical usage is when you have an InputStream for a source of encrypted data on disk, and an
	 * OutputStream to send the file to an HTTP response. You would call this method to wrap the
	 * InputStream and use the returned {@link CipherInputStream} to read the data from instead so
	 * that it is decrypted as it is read and can be written to the response unencrypted.
	 * <p>
	 * Note that this method reads and discards the random initialisation vector from the source
	 * InputStream, so the source parameter will have some bytes read from it before this method
	 * returns. These bytes are necessary for decryption and the call to
	 * {@link #encrypt(OutputStream, SecretKey)} will have added these to the start of the
	 * underlying data automatically.
	 * 
	 * @param source
	 *            The source {@link InputStream}, containing encrypted data.
	 * @param key
	 *            The key to be used for decryption.
	 * @return A {@link CipherInputStream}, which wraps the given source stream and will decrypt the
	 *         data as they are read.
	 * @throws IOException
	 *             If an error occurs in reading the initialisation vector from the source stream.
	 * @throws InvalidKeyException
	 *             If the given key is not a valid {@value #CIPHER_ALGORITHM} key.
	 */
	public InputStream decrypt(InputStream source, SecretKey key) throws IOException, InvalidKeyException {

		// Remove the random initialisation vector from the start of the stream.
		// NB if the stream is empty, the read will return -1 and no harm will be done.
		byte[] iv = new byte[cipher.getBlockSize()];
		source.read(iv);

		// Get a cipher instance and create the cipherInputStream:
		initCipher(Cipher.DECRYPT_MODE, key, iv);
		CipherInputStream cipherInputStream = new CipherInputStream(source, cipher);

		// Return the initialised stream:
		return cipherInputStream;
	}

	/**
	 * This method generates a random initialisation vector. The length of the IV is determined by
	 * calling {@link Cipher#getBlockSize()} on {@link #cipher}.
	 * 
	 * @return A byte array, of a size corresponding to the block size of the given {@link Cipher},
	 *         containing random bytes.
	 */
	byte[] generateInitialisationVector() {
		byte[] bytes = new byte[cipher.getBlockSize()];
		Random.getInstance().nextBytes(bytes);
		return bytes;
	}

	/**
	 * This method returns a {@link Cipher} instance, for {@value #CIPHER_ALGORITHM} in
	 * {@value #CIPHER_MODE} mode, with padding {@value #CIPHER_PADDING}.
	 * <p>
	 * It then initialises the {@link Cipher} in either {@link Cipher#ENCRYPT_MODE} or
	 * {@link Cipher#DECRYPT_MODE}), as specified by the mode parameter, with the given
	 * {@link SecretKey}.
	 * 
	 * @param mode
	 *            One of {@link Cipher#ENCRYPT_MODE} or {@link Cipher#DECRYPT_MODE}).
	 * @param key
	 *            The {@link SecretKey} to be used with the {@link Cipher}.
	 * @param iv
	 *            The initialisation vector to use.
	 * 
	 * @throws InvalidKeyException
	 *             If the given key is not a valid {@value #CIPHER_ALGORITHM} key.
	 */
	private void initCipher(int mode, SecretKey key, byte[] iv) throws InvalidKeyException {

		try {

			// Initialise the cipher:
			IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
			cipher.init(mode, key, ivParameterSpec);

		} catch (InvalidAlgorithmParameterException e) {
			throw new RuntimeException(
					"Invalid parameter passed to initialiset cipher for encryption: zero IvParameterSpec containing "
							+ cipher.getBlockSize() + " bytes.", e);
		}
	}
}