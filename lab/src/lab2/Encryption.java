package lab2;

import java.io.*;
import java.security.SecureRandom;

public class Encryption {
    private static final int DELTA = 0x9e3779b9;
    private static final int NUM_ROUNDS = 32;
    private static final int BLOCK_SIZE = 8; // Размер блока в байтах (64 бита)

    public static void main(String[] args) {
        // Проверка наличия аргументов командной строки
        if (args.length < 2) {
            System.out.println("Usage: java Encryption <input_file> <output_file>");
            System.exit(1);
        }

        // Имена файлов
        String inputFileName = args[0];
        String outputFileName = args[1];

        String keyFileName = "key.txt"; // Имя файла для хранения ключа
        String ivFileName = "iv.txt";   // Имя файла для хранения IV

        try {
            // Генерация ключа и IV
            generateKey(keyFileName);
            generateIV(ivFileName);

            // Шифрование файла
            encryptFile(inputFileName, outputFileName, keyFileName, ivFileName);
            System.out.println("Encryption successful.");

            // Дешифрование файла
            decryptFile(outputFileName, inputFileName.replace(".enc", ""), keyFileName, ivFileName);
            System.out.println("Decryption successful.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Метод для генерации ключа
    private static void generateKey(String keyFileName) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(keyFileName)) {
            SecureRandom secureRandom = new SecureRandom();
            byte[] key = new byte[BLOCK_SIZE * 2]; // Генерация 128-битного (16 байт) ключа
            secureRandom.nextBytes(key);
            outputStream.write(key);
        }
    }

    // Метод для генерации IV
    private static void generateIV(String ivFileName) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(ivFileName)) {
            SecureRandom secureRandom = new SecureRandom();
            byte[] iv = new byte[BLOCK_SIZE]; // Генерация 64-битного (8 байт) IV
            secureRandom.nextBytes(iv);
            outputStream.write(iv);
        }
    }

    // Метод для чтения ключа из файла
    private static int[] readKey(String keyFileName) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(keyFileName)) {
            byte[] keyBytes = inputStream.readAllBytes();
            if (keyBytes.length % 4 != 0) {
                throw new IllegalArgumentException("Invalid key length");
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

    // Метод для шифрования блока данных
    private static byte[] teaEncrypt(byte[] message, int[] key) {
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
        return result;
    }

    // Метод для шифрования файла
    private static void encryptFile(String inputFileName, String outputFileName, String keyFileName, String ivFileName) throws IOException {
        int[] key = readKey(keyFileName);
        byte[] iv = readIV(ivFileName);

        try (FileInputStream inputStream = new FileInputStream(inputFileName);
             FileOutputStream outputStream = new FileOutputStream(outputFileName)) {
            byte[] inputBytes = new byte[BLOCK_SIZE];
            byte[] outputBytes = new byte[BLOCK_SIZE];
            int bytesRead;

            while ((bytesRead = inputStream.read(inputBytes)) != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    outputBytes[i] = (byte) (inputBytes[i] ^ iv[i]);
                }
                byte[] encryptedBlock = teaEncrypt(outputBytes, key);
                outputStream.write(encryptedBlock);
                iv = encryptedBlock; // Обновление IV для следующего блока
            }
        }
    }

    // Метод для дешифрования файла
    private static void decryptFile(String inputFileName, String outputFileName, String keyFileName, String ivFileName) throws IOException {
        int[] key = readKey(keyFileName);
        byte[] iv = readIV(ivFileName);

        try (FileInputStream inputStream = new FileInputStream(inputFileName);
             FileOutputStream outputStream = new FileOutputStream(outputFileName)) {
            byte[] inputBytes = new byte[BLOCK_SIZE];
            byte[] outputBytes = new byte[BLOCK_SIZE];
            int bytesRead;

            while ((bytesRead = inputStream.read(inputBytes)) != -1) {
                byte[] decryptedBlock = teaEncrypt(inputBytes, key);
                for (int i = 0; i < bytesRead; i++) {
                    outputBytes[i] = (byte) (decryptedBlock[i] ^ iv[i]);
                }
                outputStream.write(outputBytes);
                iv = inputBytes; // Обновление IV для следующего блока
            }
        }
    }

    // Метод для чтения IV из файла
    private static byte[] readIV(String ivFileName) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(ivFileName)) {
            return inputStream.readAllBytes();
        }
    }
}
