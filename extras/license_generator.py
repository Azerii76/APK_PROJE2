#!/usr/bin/env python3
"""
Basit, bağımlılıksız lisans üretici.
SECRET_SEED gerekirse değiştirilebilir; algoritma Android tarafıyla bire bir aynıdır.
"""

SECRET_SEED = "YASIN_SUPER_SECRET_2025_XYZ"
ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"


def generate_expected_code(license_name: str, expiry_date_str: str) -> str:
    base = f"{SECRET_SEED}|{license_name}|{expiry_date_str}"
    encoded_bytes = []
    for index, ch in enumerate(base):
        val1 = (ord(ch) * 7 + index * 13 + 11) % 256
        val2 = (val1 * 5 + 19) % 256
        encoded_bytes.append(val2)
    encoded_string = "".join(ALPHABET[b % len(ALPHABET)] for b in encoded_bytes)
    trimmed = encoded_string[:16]
    blocks = [trimmed[i:i+4] for i in range(0, len(trimmed), 4)]
    return "-".join(blocks)


def main():
    license_name = input("License Name: ").strip()
    expiry_date_str = input("Expiry Date (YYYY-MM-DD): ").strip()
    code = generate_expected_code(license_name, expiry_date_str)
    print("\nLicense Name:", license_name)
    print("Expiry Date:", expiry_date_str)
    print("License Code:", code)


if __name__ == "__main__":
    main()
