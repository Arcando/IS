package lab2;

import java.io.*;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Scanner;

public class Encryption {
    private static final int DELTA = 0x9e3779b9;
    private static final int NUM_ROUNDS = 32;
    private static final int BLOCK_SIZE = 8; // Размер блока в байтах (64 бита)

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Введите мод: -e - шифрование, -d - дешифрование");
        String mode = scanner.nextLine();
        System.out.println("Введите название файла:");

        String inputFileName = "C:\\Users\\ADMIN\\Desktop\\ИБ\\ЛР2\\" + scanner.nextLine();
        String keyFileName = "C:\\Users\\ADMIN\\Desktop\\ИБ\\ЛР2\\key.txt";

        if ("-e".equals(mode)) {
            encrypt(inputFileName, keyFileName);
        } else if ("-d".equals(mode)) {
            decrypt(inputFileName, keyFileName);
        } else {
            System.out.println("Неверный режим. Используйте -e для шифрования или -d для дешифрования.");
        }
    }

    private static void encrypt(String inputFileName, String keyFileName) {
        String outputFileName = inputFileName + ".enc";
        String ivFileName = inputFileName + ".iv";

        try {
            // Генерация ключа и IV
            generateKey(keyFileName);
            generateIV(ivFileName);

            long inputFileSize = new File(inputFileName).length();
            int numBlocks = (int) (inputFileSize / BLOCK_SIZE);
            int lastBlockSize = (int) (inputFileSize % BLOCK_SIZE);

            try (FileInputStream inputStream = new FileInputStream(inputFileName);
                 FileOutputStream outputStream = new FileOutputStream(outputFileName)) {
                for (int i = 0; i < numBlocks; i++) {
                    byte[] inputBytes = new byte[BLOCK_SIZE];
                    inputStream.read(inputBytes);
                    byte[] encryptedBlock = encryptBlock(inputBytes, keyFileName);
                    outputStream.write(encryptedBlock);
                }
                // Обработка последнего блока
                if (lastBlockSize != 0) {
                    byte[] inputBytes = new byte[lastBlockSize];
                    inputStream.read(inputBytes);
                    byte[] encryptedBlock = encryptBlock(inputBytes, keyFileName);
                    outputStream.write(encryptedBlock);
                }
            }

            System.out.println("Шифрование завершено.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void decrypt(String inputFileName, String keyFileName) {
        String outputFileName = inputFileName.replace(".enc", "_decrypted.txt");
        String ivFileName = inputFileName.replace(".enc", ".iv");

        try {
            int[] key = readKey(keyFileName);
            byte[] iv = readIV(ivFileName);

            if (iv == null || iv.length != BLOCK_SIZE) {
                System.err.println("Ошибка: IV не найден или имеет неправильный формат.");
                return;
            }

            try (FileInputStream inputStream = new FileInputStream(inputFileName);
                 FileOutputStream outputStream = new FileOutputStream(outputFileName)) {
                byte[] inputBytes = inputStream.readAllBytes();

                byte[] outputBytes = new byte[inputBytes.length];
                int blockSize = BLOCK_SIZE;

                // Дешифруем каждый блок
                for (int i = 0; i < inputBytes.length; i += blockSize) {
                    byte[] encryptedBlock = Arrays.copyOfRange(inputBytes, i, i + blockSize);
                    byte[] decryptedBlock = teaDecrypt(encryptedBlock, key);

                    // XOR с предыдущим блоком (или IV для первого блока)
                    for (int j = 0; j < blockSize; j++) {
                        if (i == 0) {
                            outputBytes[i + j] = (byte) (decryptedBlock[j] ^ iv[j]);
                        } else {
                            outputBytes[i + j] = (byte) (decryptedBlock[j] ^ inputBytes[i - blockSize + j]);
                        }
                    }
                }

                // Записываем дешифрованные данные в новый файл
                outputStream.write(outputBytes);

                System.out.println("Дешифрование завершено.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    private static byte[] teaDecrypt(byte[] message, int[] key) {
        int[] v = new int[2];
        v[0] = ((message[0] << 24) | ((message[1] & 0xFF) << 16) | ((message[2] & 0xFF) << 8) | (message[3] & 0xFF));
        v[1] = ((message[4] << 24) | ((message[5] & 0xFF) << 16) | ((message[6] & 0xFF) << 8) | (message[7] & 0xFF));
        int sum = DELTA * NUM_ROUNDS;

        for (int i = 0; i < NUM_ROUNDS; i++) {
            v[1] -= ((v[0] << 4) + key[2]) ^ (v[0] + sum) ^ ((v[0] >>> 5) + key[3]);
            v[0] -= ((v[1] << 4) + key[0]) ^ (v[1] + sum) ^ ((v[1] >>> 5) + key[1]);
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

    private static void generateKey(String keyFileName) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(keyFileName)) {
            SecureRandom secureRandom = new SecureRandom();
            byte[] key = new byte[BLOCK_SIZE * 2]; // 16 bytes key
            secureRandom.nextBytes(key);
            outputStream.write(key);
        }
    }

    private static void generateIV(String ivFileName) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(ivFileName)) {
            SecureRandom secureRandom = new SecureRandom();
            byte[] iv = new byte[BLOCK_SIZE]; // 8 bytes IV
            secureRandom.nextBytes(iv);
            outputStream.write(iv);
        }
    }
    private static int[] readKey(String keyFileName) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(keyFileName)) {
            byte[] keyBytes = inputStream.readAllBytes();
            if (keyBytes.length % 4 != 0) {
                throw new IllegalArgumentException("Недопустимая длина ключа");
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

    private static byte[] teaEncrypt(byte[] message, int[] key) {
        int[] v = new int[2];
        v[0] = ((message[0] << 24) | ((message[1] & 0xFF) << 16) | ((message[2] & 0xFF) << 8) | (message[3] & 0xFF));
        v[1] = ((message[4] << 24) | ((message[5] & 0xFF) << 16) | ((message[6] & 0xFF) << 8) | (message[7] & 0xFF));
        int sum = 0;

        for (int i = 0; i < NUM_ROUNDS; i++) {
            sum += DELTA;
            v[0] += ((v[1] << 4) + key[0]) ^ (v[1] + sum) ^ ((v[1] >>> 5) + key[1]);
            v[1] += ((v[0] << 4) + key[2]) ^ (v[0] + sum) ^ ((v[0] >>> 5) + key[3]);
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

    private static byte[] encryptBlock(byte[] message, String keyFileName) throws IOException {
        int[] key = readKey(keyFileName);
        return teaEncrypt(message, key);
    }
}

