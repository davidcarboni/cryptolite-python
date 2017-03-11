"""
    This module generates cryptographic keys.

    The following key types are available:
    - Deterministic symmetric/secret 256-bit AES keys, based on a password
    - Random symmetric/secret 256-bit AES keys
    - Asymmetric 3072-bit RSA key pairs
    
    *Deterministic keys:* these are the easiest to manage as they don't need to be stored. So
    long as you pass in the same password each time, the same key will be generated every time. The
    drawback is that if you want to generate more than one key you'll need more than one password.
    However, if you do only need one key, this approach can be ideal as you can use the user's
    plaintext password to generate the key. Since you never store a user's plaintext password (see
    ``Password.hash(String)``) the key can only be regenerated using the correct password. Bear
    in mind however that if the user changes (or resets) their password this will result in a
    different key, so you'll need a plan for recovering data encrypted with the old key and
    re-encrypting it with the new one.

    *Random keys:* these are simple to generate, but need to be stored because it's
    effectively impossible to regenerate the key. To store a key you should use
    ``KeyWrapper.wrapSecretKey(SecretKey)``. This produces an encrypted version of the key which
    can safely be stored in, for example, a database or configuration value. The benefit of the
    ``KeyWrapper`` approach is that when a user changes their password you'll only need to
    re-encrypt the stored keys using a ``KeyWrapper`` initialised with the new password, rather
    than have to re-encrypt all data encrypted with the key.

    In both cases when a user changes their password you will have the old and the new plaintext
    passwords, meaning you can decrypt with the old an re-encrypt with the new. The difficulty comes
    when you need to reset a password, because it's not possible to recover the old password. In this
    case you either need a secondary password, such as a security question, or you need to be clear
    that data cannot be recovered. Whatever your solution, remember that storing someone's password
    in any recoverable form is not OK, so you'll need to put some thought into the recovery process.
 """

import os
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.backends import default_backend
from cryptolite import generate_random
from cryptolite import byte_array

__author__ = "David Carboni"

backend = default_backend()

"""The symmetric key algorithm."""
SYMMETRIC_ALGORITHM = "AES"

"""The key size for symmetric keys."""
SYMMETRIC_KEY_SIZE = 256

"""The algorithm to use to generate password-based secret keys."""
SYMMETRIC_PASSWORD_ALGORITHM = "PBKDF2WithHmacSHA1"

"""The number of iterations to use for password-based key derivation."""
SYMMETRIC_PASSWORD_ITERATIONS = 1024

"""The asymmetric key algorithm."""
ASYMMETRIC_ALGORITHM = "RSA"

"""The key size for asymmetric keys."""
ASYMMETRIC_KEY_SIZE = 3072


def new_secret_key():
    """Generates a new secret (or symmetric) key for use with AES.

        The key size is determined by SYMMETRIC_KEY_SIZE.

        :return: A new, randomly generated secret key.
    """
    # FYI: AES keys are just random bytes from a strong source of randomness.
    return os.urandom(SYMMETRIC_KEY_SIZE // 8)


def generate_secret_key(password, salt):
    """Generates a new secret (or symmetric) key for use with SYMMETRIC_ALGORITHM using the given password and salt values.

    Given the same password and salt, this method will (re)generate the same key.

    Note that this method may or may not handle blank passwords. This seems to be related to the
    implementation of the SYMMETRIC_PASSWORD_ALGORITHM.

    :param password: The starting point to use in generating the key. This can be a password, or any
                    suitably secret string. It's worth noting that, if a user's plaintext password is
                    used, this makes key derivation secure, but means the key can never be recovered
                    if a user forgets their password. If a different value, such as a password hash is
                    used, this is not really secure, but does mean the key can be recovered if a user
                    forgets their password. It's a trade-off, right?
    :param salt:     A value for this parameter can be generated by calling
                    generate_random.salt(). You'll need to store the salt value (this is ok to do
                    because salt isn't particularly sensitive) and use the same salt each time in
                    order to always generate the same key. Using salt is good practice as it ensures
                    that keys generated from the same password will be different - i.e. if two users
                    use the password, having a salt value avoids the generated keys being
                    identical which might give away someone's password.
    :return: A deterministic secret key, defined by the given password and salt
    """
    if password is None:
        return None
    salt_bytes = bytes(byte_array.from_base64_string(salt))
    key_generator = PBKDF2HMAC(
        algorithm=hashes.SHA256(),
        length=SYMMETRIC_KEY_SIZE // 8,
        salt=salt_bytes,
        iterations=SYMMETRIC_PASSWORD_ITERATIONS,
        backend=backend
    )
    return key_generator.derive(password.encode("utf-8"))


def new_key_pair():
    """Generates a new public-private (or asymmetric) key pair for use with ASYMMETRIC_ALGORITHM.

    The key size will be ASYMMETRIC_KEY_SIZE bits.

    :return: A new, randomly generated asymmetric key pair.
    """
    return rsa.generate_private_key(
        public_exponent=65537,
        key_size=ASYMMETRIC_KEY_SIZE,
        backend=default_backend()
    )
