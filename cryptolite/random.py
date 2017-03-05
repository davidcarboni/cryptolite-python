"""
This module provides random functions, such as Salt, token and password
 generation. It also allows you to get a singleton {@link SecureRandom}
 instance.
"""

import os
import string
from secrets import choice
from cryptolite.byte_array import to_hex_string, to_base64_string

__author__ = "David Carboni"

"""The length of tokens."""
TOKEN_BITS = 256

"""The length of salt values."""
SALT_BYTES = 16

# Work out the right number of bytes for random tokens:
_bits_in_a_byte = 8
_token_length_bytes = TOKEN_BITS // _bits_in_a_byte

# Characters for pasword generation:
passwordCharacters = string.ascii_letters + string.digits


def byte_array(length):
    """Convenience method to instantiate and populate a byte array of the specified length.

         :param length: The length of the array.
         :return: os.urandom(length)
    """
    return bytearray(os.urandom(length))


def input_stream(length):
    """Convenience method to instantiate an {@link InputStream} of random data of the specified length.
         :param length: The length of the stream.
         :return: An {@link InputStream} which will provide the specified number of random bytes.
    """
    pass


def token():
    """Generates a random token.
    :return: A 256-bit (32 byte) random token as a hexadecimal string.
    """
    token_bytes = byte_array(_token_length_bytes)
    return to_hex_string(token_bytes)


def password(length):
    """Generate a random password.

    :param length: The length of the password to be returned.
    :return: A password of the specified length, selected from passwordCharacters.
    """
    return ''.join(choice(passwordCharacters) for i in range(length))


def salt():
    """Generates a random salt value.

    If a salt value is needed by an API call,
    the documentation of that method should reference this method. Other than than,
    it should not be necessary to call this in normal usage of this library.

    :return: A random salt value of SALT_BYTES length, as a base64-encoded
    string (for easy storage).
    """
    salt = byte_array(SALT_BYTES)
    return to_base64_string(salt)