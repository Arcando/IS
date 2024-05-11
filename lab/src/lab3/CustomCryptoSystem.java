package lab3;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

import java.io.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class CustomCryptoSystem {

    private static final int NUM_ROUNDS = 32;
    private static final int BLOCK_SIZE = 8; // 64 bits

    // Генерация случайного вектора инициализации
    private static void generateIV(int[] iv) {
        Random random = new Random();
        for (int i = 0; i < 2; ++i) {
            iv[i] = random.nextInt();
        }
    }

    // Функция шифрования блока данных по алгоритму TEA
    private static void teaEncrypt(int[] v, int[] k) {
        int v0 = v[0], v1 = v[1], sum = 0;
        int delta = 0x9e3779b9;
        int k0 = k[0], k1 = k[1], k2 = k[2], k3 = k[3];

        for (int i = 0; i < NUM_ROUNDS; i++) {
            sum += delta;
            v0 += ((v1 << 4) + k0) ^ (v1 + sum) ^ ((v1 >>> 5) + k1);
            v1 += ((v0 << 4) + k2) ^ (v0 + sum) ^ ((v0 >>> 5) + k3);
        }

        v[0] = v0;
        v[1] = v1;
    }

    // Функция дешифрования блока данных по алгоритму TEA
    private static void teaDecrypt(int[] v, int[] k) {
        int v0 = v[0], v1 = v[1], sum = 0xC6EF3720;
        int delta = 0x9e3779b9;
        int k0 = k[0], k1 = k[1], k2 = k[2], k3 = k[3];

        for (int i = 0; i < NUM_ROUNDS; i++) {
            v1 -= ((v0 << 4) + k2) ^ (v0 + sum) ^ ((v0 >>> 5) + k3);
            v0 -= ((v1 << 4) + k0) ^ (v1 + sum) ^ ((v1 >>> 5) + k1);
            sum -= delta;
        }

        v[0] = v0;
        v[1] = v1;
    }

    // Генерация и сохранение случайного ключа
    private static void generateAndSaveKey(int[] key) {
        Random random = new Random();
        for (int i = 0; i < 4; ++i) {
            key[i] = random.nextInt();
        }
    }

    // Шифрование ключа с использованием хеша пароля
    private static void encryptKey(int[] originalKey, int[] passwordHash, int[] encryptedKey) {
        for (int i = 0; i < 4; ++i) {
            encryptedKey[i] = originalKey[i] ^ passwordHash[i];
        }
    }

    // Дешифрование ключа с использованием хеша пароля
    private static void decryptKey(int[] encryptedKey, int[] passwordHash, int[] decryptedKey) {
        for (int i = 0; i < 4; ++i) {
            decryptedKey[i] = encryptedKey[i] ^ passwordHash[i];
        }
    }

    // XOR операция массива данных с ключом
    private static void xorArray(byte[] data, int[] key) {
        for (int i = 0; i < data.length; i++) {
            data[i] ^= (byte) ((key[i / 4] >> ((i % 4) * 8)) & 0xFF);
        }
    }

    // Операция Output Feedback (OFB)
    private static void ofb(byte[] data, int[] key, int[] iv) {
        byte[] state = new byte[BLOCK_SIZE];
        for (int i = 0; i < 2; i++) {
            state[i] = (byte) (iv[i] & 0xFF);
        }

        for (int i = 0; i < data.length; i += BLOCK_SIZE) {
            xorArray(state, key);
            for (int j = 0; j < BLOCK_SIZE; j++) {
                if (i + j < data.length) {
                    data[i + j] ^= state[j];
                }
            }
        }
    }

    // Хеширование пароля
    private static void hashPassword(String password, int[] hash) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] passwordBytes = password.getBytes();
        byte[] digest = md.digest(passwordBytes);

        for (int i = 0; i < 4; i++) {
            hash[i] = ByteBuffer.wrap(Arrays.copyOfRange(digest, i * 4, (i + 1) * 4)).getInt();
        }
    }

    // Функция шифрования файла
    private static void encryptFile(String inputFile, String outputFile, int[] originalKey, int[] iv) throws Exception {
        FileInputStream inputStream = new FileInputStream(inputFile);
        byte[] inputBytes = inputStream.readAllBytes();
        inputStream.close();

        for (int i = 0; i < inputBytes.length; i += BLOCK_SIZE) {
            if (i + BLOCK_SIZE <= inputBytes.length) {
                int[] block = new int[2];
                block[0] = (inputBytes[i] << 24) | ((inputBytes[i + 1] & 0xFF) << 16) | ((inputBytes[i + 2] & 0xFF) << 8) | (inputBytes[i + 3] & 0xFF);
                block[1] = (inputBytes[i + 4] << 24) | ((inputBytes[i + 5] & 0xFF) << 16) | ((inputBytes[i + 6] & 0xFF) << 8) | (inputBytes[i + 7] & 0xFF);
                teaEncrypt(block, originalKey);
                inputBytes[i] = (byte) (block[0] >>> 24);
                inputBytes[i + 1] = (byte) (block[0] >>> 16);
                inputBytes[i + 2] = (byte) (block[0] >>> 8);
                inputBytes[i + 3] = (byte) block[0];
                inputBytes[i + 4] = (byte) (block[1] >>> 24);
                inputBytes[i + 5] = (byte) (block[1] >>> 16);
                inputBytes[i + 6] = (byte) (block[1] >>> 8);
                inputBytes[i + 7] = (byte) block[1];
            } else {
                byte[] block = Arrays.copyOfRange(inputBytes, i, inputBytes.length);
                ofb(block, originalKey, iv);
                System.arraycopy(block, 0, inputBytes, i, block.length);
                break;
            }
        }

        FileOutputStream outputStream = new FileOutputStream(outputFile);
        outputStream.write(inputBytes);
        outputStream.close();
    }

    // Функция дешифрования файла
    private static void decryptFile(String inputFile, String outputFile, int[] passwordDecryptedKey, int[] iv) throws Exception {
        FileInputStream inputStream = new FileInputStream(inputFile);
        byte[] inputBytes = inputStream.readAllBytes();
        inputStream.close();

        int keySize = 4 * Integer.BYTES;
        if (inputBytes.length < keySize) {
            System.out.println("File does not contain correctly encrypted data.");
            return;
        }

        byte[] dataBytes = Arrays.copyOf(inputBytes, inputBytes.length - keySize);

        for (int i = 0; i < dataBytes.length; i += BLOCK_SIZE) {
            int[] block = new int[2];
            block[0] = (dataBytes[i] << 24) | ((dataBytes[i + 1] & 0xFF) << 16) | ((dataBytes[i + 2] & 0xFF) << 8) | (dataBytes[i + 3] & 0xFF);
            block[1] = (dataBytes[i + 4] << 24) | ((dataBytes[i + 5] & 0xFF) << 16) | ((dataBytes[i + 6] & 0xFF) << 8) | (dataBytes[i + 7] & 0xFF);

            if (i + BLOCK_SIZE <= dataBytes.length) {
                teaDecrypt(block, passwordDecryptedKey);
                dataBytes[i] = (byte) (block[0] >>> 24);
                dataBytes[i + 1] = (byte) (block[0] >>> 16);
                dataBytes[i + 2] = (byte) (block[0] >>> 8);
                dataBytes[i + 3] = (byte) block[0];
                dataBytes[i + 4] = (byte) (block[1] >>> 24);
                dataBytes[i + 5] = (byte) (block[1] >>> 16);
                dataBytes[i + 6] = (byte) (block[1] >>> 8);
                dataBytes[i + 7] = (byte) block[1];
            } else {
                byte[] blockBytes = Arrays.copyOfRange(dataBytes, i, dataBytes.length);
                ofb(blockBytes, passwordDecryptedKey, iv);
                System.arraycopy(blockBytes, 0, dataBytes, i, blockBytes.length);
                break;
            }
        }

        FileOutputStream outputStream = new FileOutputStream(outputFile);
        outputStream.write(dataBytes);
        outputStream.close();
    }

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter mode: -e for encryption, -d for decryption");
        String mode = scanner.nextLine();
        System.out.println("Enter file name:");
        String fileName = scanner.nextLine();
        String inputFile = "C:\\Users\\ADMIN\\Desktop\\ИБ\\ЛР3\\" + fileName;

        int[] iv = new int[2];
        generateIV(iv);

        int[] originalKey = new int[4];
        generateAndSaveKey(originalKey);

        if (mode.equals("-e")) {
            System.out.println("Enter password:");
            String password = scanner.nextLine();
            int[] passwordHash = new int[4];
            hashPassword(password, passwordHash);

            int[] encryptedKey = new int[4];
            encryptKey(originalKey, passwordHash, encryptedKey);

            encryptFile(inputFile, "C:\\Users\\ADMIN\\Desktop\\ИБ\\ЛР3\\lab3.txt.enc", originalKey, iv);

            FileOutputStream outputStream = new FileOutputStream("C:\\Users\\ADMIN\\Desktop\\ИБ\\ЛР3\\lab3_key.enc");
            for (int i = 0; i < encryptedKey.length; i++) {
                outputStream.write(ByteBuffer.allocate(4).putInt(encryptedKey[i]).array());
            }
            outputStream.close();

            System.out.println("File encrypted successfully.");
        } else if (mode.equals("-d")) {
            System.out.println("Enter password:");
            String password = scanner.nextLine();
            int[] passwordHash = new int[4];
            hashPassword(password, passwordHash);

            int[] encryptedKey = new int[4];
            FileInputStream keyStream = new FileInputStream("C:\\Users\\ADMIN\\Desktop\\ИБ\\ЛР3\\lab3_key.enc");
            for (int i = 0; i < encryptedKey.length; i++) {
                byte[] keyBytes = new byte[4];
                keyStream.read(keyBytes);
                encryptedKey[i] = ByteBuffer.wrap(keyBytes).getInt();
            }
            keyStream.close();

            int[] passwordDecryptedKey = new int[4];
            decryptKey(encryptedKey, passwordHash, passwordDecryptedKey);

            decryptFile(inputFile, "C:\\Users\\ADMIN\\Desktop\\ИБ\\ЛР3\\lab3.txt.dec", passwordDecryptedKey, iv);

            System.out.println("File decrypted successfully.");
        } else {
            System.out.println("Invalid mode.");
        }

        scanner.close();
    }
}
