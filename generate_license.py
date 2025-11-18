#!/usr/bin/env python3
"""
OtoService Lisans Anahtarı Üretici (v4 - Kullanıcı Dostu)
========================================================
Bu script, çift tıklanarak çalıştırıldığında interaktif olarak
cihaz ID'si ve geçerlilik süresi alarak lisans anahtarı üretir.

Kullanım:
    Dosyaya çift tıklayarak çalıştırın.
"""

import sys
import hashlib
import base64
from datetime import datetime, timedelta

def install_and_import(package):
    import importlib
    try:
        importlib.import_module(package)
    except ImportError:
        import pip
        pip.main(['install', package])
    finally:
        globals()[package] = importlib.import_module(package)

try:
    from Crypto.Cipher import AES
    from Crypto.Util.Padding import pad
except ImportError:
    print("Gerekli 'pycryptodome' kütüphanesi kuruluyor...")
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "pycryptodome"])
    print("Kütüphane kuruldu, lütfen scripti yeniden çalıştırın.")
    sys.exit(1)

def generate_seed(device_id):
    salt = "OTSRV!@#".encode()
    base = device_id.encode()
    return salt + base + salt[::-1]

def generate_aes_key(device_id):
    seed = generate_seed(device_id)
    digest = hashlib.sha256(seed).digest()
    return digest[:16]

def generate_signature(device_id, expire_timestamp_ms):
    value = f"{device_id}|{expire_timestamp_ms}"
    r1_value = "OTSRV_S1"
    a = hashlib.sha256(value.encode()).digest()
    b = hashlib.sha256((value[::-1] + r1_value).encode()).digest()
    signature_bytes = a + b
    return base64.b64encode(signature_bytes).decode().strip()

def encrypt_license_content(plain_text, device_id):
    key_bytes = generate_aes_key(device_id)
    cipher = AES.new(key_bytes, AES.MODE_ECB)
    padded_data = pad(plain_text.encode(), AES.block_size)
    encrypted_bytes = cipher.encrypt(padded_data)
    return base64.b64encode(encrypted_bytes).decode()

def date_to_timestamp_ms(date_str):
    dt = datetime.strptime(date_str, '%Y-%m-%d')
    dt = dt.replace(hour=23, minute=59, second=59, microsecond=999000)
    return int(dt.timestamp() * 1000)

def generate_license_key(device_id, expire_date_str):
    expire_timestamp = date_to_timestamp_ms(expire_date_str)
    signature = generate_signature(device_id, expire_timestamp)
    license_content = f"{device_id}|{expire_timestamp}|{signature}"
    encrypted_license = encrypt_license_content(license_content, device_id)
    return encrypted_license

def main():
    try:
        print("=" * 60)
        print("OtoService Lisans Anahtarı Üretici (v4 - Kullanıcı Dostu)")
        print("=" * 60)

        device_id = input("Lütfen lisanslanacak Cihaz ID'sini girin: ").strip()
        if not device_id:
            raise ValueError("Cihaz ID'si boş bırakılamaz.")

        days_str = input(f"Lisans kaç gün geçerli olsun? (örn: 30): ").strip()
        days_valid = int(days_str)
        if days_valid <= 0:
            raise ValueError("Gün sayısı pozitif bir tam sayı olmalı.")

        expire_date_obj = datetime.now() + timedelta(days=days_valid)
        expire_date_str = expire_date_obj.strftime('%Y-%m-%d')

        print("-" * 25)
        print(f"Cihaz ID: {device_id}")
        print(f"Lisans Süresi: {days_valid} gün")
        print(f"Hesaplanan Bitiş Tarihi: {expire_date_str}")
        print("-" * 25)
        print("Lisans anahtarı üretiliyor...")

        license_key = generate_license_key(device_id, expire_date_str)

        print("\n" + "=" * 60)
        print("LİSANS ANAHTARI BAŞARIYLA OLUŞTURULDU")
        print("=" * 60)
        print(f"\n{license_key}\n")
        print("=" * 60)

    except Exception as e:
        print(f"\n❌ HATA: {str(e)}")
    finally:
        print("\nKapatmak için Enter tuşuna basın...")
        input() # Pencerenin hemen kapanmasını engeller

if __name__ == "__main__":
    main()
