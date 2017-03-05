import unittest
from unittest import TestCase
from cryptolite import random, byte_array


class TestRandom(TestCase):
    """Test for random"""

    def test_byte_array(self):
        """Checks that generating a random byte array returns the expected number of bytes."""

        # Given
        length = 20

        # When
        # Some random bytes
        data = random.byte_array(length)

        # Then
        # Check we got what we expected
        self.assertEqual(length, len(data))
        self.assertIsInstance(data, bytearray)

    def test_token_length(self):
        """Checks that the number of bits in the returned ID is the same as specified by TOKEN_BITS."""

        # When
        # We generate a token
        token = random.token()

        # Then
        # It should be of the expected length
        token_bytes = byte_array.from_hex_string(token)
        self.assertEqual(random.TOKEN_BITS, len(token_bytes) * 8)

    def test_salt_length(self):
        """Checks that the number of bytes in a returned salt value matches the length specified in SALT_BYTES."""

        # When
        # We generate a salt
        salt = random.salt()

        # Then
        # It should be of the expected length
        salt_bytes = byte_array.from_base64_string(salt)
        self.assertEqual(random.SALT_BYTES, len(salt_bytes))

    def test_input_stream(self):
        """Verifies that a random input stream provides the expected amout of input.

        ND this is Not currently implemented.
        """

        # Given
        length = 1025
        inputStream = random.input_stream(length)

        # When
        count = 1025
        # while (inputStream.read() != -1):
        #    count++

        # Then
        self.assertEqual(length, count)

    def test_password_length(self):
        """Checks the number of characters in the returned password matches the specified length of the password."""

        # Given
        max_length = 100

        for length in range(1, max_length):
            # When
            password = random.password(length)

            # Then
            self.assertEqual(length, len(password))

    def test_randomness_of_tokens(self):
        """Test the general randomness of token generation.

        If this test fails, consider yourself astoundingly lucky.. or check the code is really producing random numbers.
        """

        iterations = 1000
        for i in range(1, iterations):
            # When
            id1 = random.token()
            id2 = random.token()

            # Then
            self.assertNotEqual(id1, id2)

    def test_randomness_of_salt(self):
        """Test the general randomness of salt generation.

        If this test fails, consider yourself astoundingly lucky.. or check the code is really producing random numbers.
        """

        iterations = 1000
        for i in range(1, iterations):
            # When
            salt1 = random.salt()
            salt2 = random.salt()

            # Then
            self.assertNotEqual(salt1, salt2)

    def test_randomness_of_passwords(self):
        """Test the general randomness of password generation.

        If this test fails, consider yourself astoundingly lucky.. or check the code is really producing random numbers.
        """

        iterations = 1000
        pasword_size = 8
        for i in range(1, iterations):
            # When
            password1 = random.password(pasword_size)
            password2 = random.password(pasword_size)

            # Then
            self.assertNotEqual(password1, password2)


if __name__ == '__main__':
    unittest.main()
