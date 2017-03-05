import unittest
from unittest import TestCase
from cryptolite import byte_array


class TestByteArray(TestCase):
    def setUp(self):
        self.data = bytearray("Mary had a little Café".encode("UTF8"))

    def test_hex(self):
        """Verifies a byte array can be correctly converted to a hex String and back again."""

        # Given
        # The byte array from setup

        # When
        # We convert to hex and back again
        hex_string = byte_array.to_hex_string(self.data)
        back_again = byte_array.from_hex_string(hex_string)

        # Then
        # The end result should match the input
        self.assertEqual(self.data, back_again)
        self.assertIsInstance(hex_string, str)
        self.assertIsInstance(back_again, bytearray)

    def test_hex_prefix(self):
        """Verifies that a hex String with a 0x prefix can be correctly converted to bytes."""

        # Given
        # A hex string with a 0x prefix
        hex_string = "0x" + byte_array.to_hex_string(self.data)

        # When
        # We attempt to convert to a byte array
        actual = byte_array.from_hex_string(hex_string)

        # Then
        # No error should occur
        self.assertEqual(self.data, actual)

    def test_hex_none(self):
        """Verifies that None is gracefully handled."""

        # When
        # We attempt conversion
        b = byte_array.to_hex_string(None)
        s = byte_array.from_hex_string(None)

        # Then
        # No error should occur and we should have None results
        self.assertEqual(None, b)
        self.assertEqual(None, s)

    def test_base64(self):
        """Verifies a byte array can be correctly converted to base64 and back again."""

        # Given
        # The byte array from setup

        # When
        # We convert to hex and back again
        base64_string = byte_array.to_base64_string(self.data)
        back_again = byte_array.from_base64_string(base64_string)

        # Then
        # The end result should match the input
        self.assertEqual(self.data, back_again)
        self.assertIsInstance(base64_string, str)
        self.assertIsInstance(back_again, bytearray)

    def test_base64_none(self):
        """Verifies that None is gracefully handled."""

        # When
        # We attempt conversion
        b = byte_array.to_base64_string(None)
        s = byte_array.from_base64_string(None)

        # Then
        # No error should occur and we should have None results
        self.assertEqual(None, b)
        self.assertEqual(None, s)

    def test_string(self):
        """Verifies a byte array can be correctly converted to a string and back again."""

        # Given
        # The byte array from setup

        # When
        # We convert to string and back again
        string = byte_array.to_string(self.data)
        back_again = byte_array.from_string(string)

        # Then
        # The end result should match the input
        self.assertEqual(self.data, back_again)
        self.assertIsInstance(string, str)
        self.assertIsInstance(back_again, bytearray)

    def test_string_none(self):
        """Verifies that None is gracefully handled."""

        # When
        # We attempt conversion
        b = byte_array.to_string(None)
        s = byte_array.from_string(None)

        # Then
        # No error should occur and we should have None results
        self.assertEqual(None, b)
        self.assertEqual(None, s)


if __name__ == '__main__':
    unittest.main()
