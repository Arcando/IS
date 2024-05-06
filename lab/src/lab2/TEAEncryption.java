package lab2;

import java.io.*;
import java.security.SecureRandom;
import java.util.Scanner;

public class TEAEncryption {
    private static final int DELTA = 0x9e3779b9;
    private static final int NUM_ROUNDS = 32;
    private static final int BLOCK_SIZE = 8; // 64 bits
    private static final long MIN_FILE_SIZE = 4 * 1024 * 1025; // 4 MB

    private static void generateKey(String keyFileName) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(keyFileName)) {
            SecureRandom secureRandom = new SecureRandom();
            byte[] key = new byte[BLOCK_SIZE * 2];
            secureRandom.nextBytes(key);
            outputStream.write(key);
        }
    }

    private static void generateIV(String ivFileName) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(ivFileName)) {
            SecureRandom secureRandom = new SecureRandom();
            byte[] iv = new byte[BLOCK_SIZE];
            secureRandom.nextBytes(iv);
            outputStream.write(iv);
        }
    }

    private static byte[] teaEncrypt(byte[] message, int[] key, byte[] iv) {
        int[] v = new int[2];
        v[0] = ((message[0] << 24) | ((message[1] & 0xFF) << 16) | ((message[2] & 0xFF) << 8) | (message[3] & 0xFF));
        v[1] = ((message[4] << 24) | ((message[5] & 0xFF) << 16) | ((message[6] & 0xFF) << 8) | (message[7] & 0xFF));
        int sum = 0;

        for (int i = 0; i < NUM_ROUNDS; i++) {
            sum += DELTA;
            v[0] += ((v[1] << 4) + key[0]) ^ (v[1] + sum) ^ ((v[1] >> 5) + key[1]);
            v[1] += ((v[0] << 4) + key[2]) ^ (v[0] + sum) ^ ((v[0] >> 5) + key[3]);
        }

        byte[] result = new byte[BLOCK_SIZE];
        result[0] = (byte) (v[0] >>> 24);
        result[1] = (byte) (v[0] >>> 16);
        result[2] = (byte) (v[0] >>> 8);
        result[3] = (byte) v[0];
        result[4] = (byte) (v[1] >>> 24);
        result[5] = (byte) (v[1] >>> 16);
        result[6] = (byte) (v[1] >>> 8);
        result[7] = (byte) v[1];

        for (int i = 0; i < BLOCK_SIZE; i++) {
            result[i] ^= iv[i];
        }

        return result;
    }

    private static void encryptFile(String inputFileName, String outputFileName, String keyFileName, String ivFileName) throws IOException {
        File inputFile = new File(inputFileName);
        if (!inputFile.exists() || !inputFile.isFile()) {
            throw new FileNotFoundException("Файл не найден: " + inputFileName);
        }

        int[] key = readKey(keyFileName);
        byte[] iv = readIV(ivFileName);

        try (FileInputStream inputStream = new FileInputStream(inputFileName);
             FileOutputStream outputStream = new FileOutputStream(outputFileName)) {
            byte[] inputBytes = new byte[BLOCK_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(inputBytes)) != -1) {
                if (bytesRead < BLOCK_SIZE) {
                    byte[] paddedBytes = new byte[BLOCK_SIZE];
                    System.arraycopy(inputBytes, 0, paddedBytes, 0, bytesRead);
                    for (int i = bytesRead; i < BLOCK_SIZE; i++) {
                        paddedBytes[i] = 1;
                    }
                    inputBytes = paddedBytes;
                }
                byte[] encryptedBytes = teaEncrypt(inputBytes, key, iv);
                outputStream.write(encryptedBytes);
            }
        }
    }

    private static byte[] teaDecrypt(byte[] cipher, int[] key, byte[] iv) {
        for (int i = 0; i < BLOCK_SIZE; i++) {
            cipher[i] ^= iv[i];
        }

        int[] v = new int[2];
        v[0] = ((cipher[0] << 24) | ((cipher[1] & 0xFF) << 16) | ((cipher[2] & 0xFF) << 8) | (cipher[3] & 0xFF));
        v[1] = ((cipher[4] << 24) | ((cipher[5] & 0xFF) << 16) | ((cipher[6] & 0xFF) << 8) | (cipher[7] & 0xFF));
        int sum = DELTA * NUM_ROUNDS;

        for (int i = 0; i < NUM_ROUNDS; i++) {
            v[1] -= ((v[0] << 4) + key[2]) ^ (v[0] + sum) ^ ((v[0] >> 5) + key[3]);
            v[0] -= ((v[1] << 4) + key[0]) ^ (v[1] + sum) ^ ((v[1] >> 5) + key[1]);
            sum -= DELTA;
        }

        byte[] result = new byte[BLOCK_SIZE];
        result[0] = (byte) (v[0] >>> 24);
        result[1] = (byte) (v[0] >>> 16);
        result[2] = (byte) (v[0] >>> 8);
        result[3] = (byte) v[0];
        result[4] = (byte) (v[1] >>> 24);
        result[5] = (byte) (v[1] >>> 16);
        result[6] = (byte) (v[1] >>> 8);
        result[7] = (byte) v[1];
        return result;
    }

    private static void decryptFile(String inputFileName, String keyFileName, String ivFileName) throws IOException {
        File inputFile = new File(inputFileName);
        if (!inputFile.exists() || !inputFile.isFile()) {
            throw new FileNotFoundException("Файл не найден: " + inputFileName);
        }

        int[] key = readKey(keyFileName);
        byte[] iv = readIV(ivFileName);
        try (FileInputStream inputStream = new FileInputStream(inputFileName);
             FileOutputStream outputStream = new FileOutputStream(inputFileName.replace(".txt.enc", "(1).txt"))) {
            byte[] inputBytes = new byte[BLOCK_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(inputBytes)) != -1) {
                byte[] decryptedBytes = teaDecrypt(inputBytes, key, iv);
                if (inputStream.available() == 0) {
                    decryptedBytes = removePadding(decryptedBytes);
                }
                outputStream.write(decryptedBytes);
            }
        }
    }

    private static byte[] removePadding(byte[] decryptedBytes) {
        int paddingCount = 0;
        for (int i = decryptedBytes.length - 1; i >= 0; i--) {
            if (decryptedBytes[i] == 1) {
                paddingCount++;
            } else {
                break;
            }
        }
        byte[] result = new byte[decryptedBytes.length - paddingCount];
        System.arraycopy(decryptedBytes, 0, result, 0, result.length);
        return result;
    }

    private static int[] readKey(String keyFileName) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(keyFileName)) {
            byte[] keyBytes = inputStream.readAllBytes();
            if (keyBytes.length % 4 != 0) {
                throw new IllegalArgumentException("Невалидная длина ключа");
            }
            int[] key = new int[keyBytes.length / 4];
            for (int i = 0; i < keyBytes.length / 4; i++) {
                int value = 0;
                for (int j = 0; j < 4; j++) {
                    value |= (keyBytes[i * 4 + j] & 0xFF) << (24 - 8 * j);
                }
                key[i] = value;
            }
            return key;
        }
    }

    private static byte[] readIV(String ivFileName) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(ivFileName)) {
            return inputStream.readAllBytes();
        }
    }

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Введите тип действия: -e - шифрование, -d - дешифрование");
        String mode = scanner.nextLine();
        System.out.println("Введите имя файла:");

        String messageFileName = "C:\\Users\\ADMIN\\Desktop\\ИБ\\ЛР2\\" + scanner.nextLine();
        String keyFileName = "C:\\Users\\ADMIN\\Desktop\\ИБ\\ЛР2\\key.txt";
        String ivFileName = "C:\\Users\\ADMIN\\Desktop\\ИБ\\ЛР2\\iv.txt";

        if (mode.equals("-e")) {
            generateKey(keyFileName);
            generateIV(ivFileName);
            encryptFile(messageFileName, messageFileName + ".enc", keyFileName, ivFileName);
            System.out.println("Шифрование завершено");
        } else if (mode.equals("-d")) {
            decryptFile(messageFileName, keyFileName, ivFileName);
            System.out.println("Дешифрование завершено");
        } else {
            System.out.println("Некорректное действие");
        }

        scanner.close();
    }
}
