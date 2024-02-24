package main.lab;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

public class TEAEncryption {
    private static final int DELTA = 0x9e3779b9;
    private static final int NUM_ROUNDS = 32;
    private static final int BLOCK_SIZE = 8; // 64 bits

    /**
     * Метод считывает и проверяет на валидность ключ шифрования (ровно 16 байт)
     *
     * @param keyFileName
     * @return
     * @throws IOException
     */
    private static int[] readKey(String keyFileName) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(keyFileName)) {
            byte[] keyBytes = inputStream.readAllBytes();
            if (keyBytes.length % 4 != 0) {
                throw new IllegalArgumentException("Key length does not match expected length");
            }
            int[] key = new int[keyBytes.length / 4];
            for (int i = 0; i < key.length; i++) {
                key[i] = (keyBytes[i * 4] << 24) | ((keyBytes[i * 4 + 1] & 0xFF) << 16) | ((keyBytes[i * 4 + 2] & 0xFF) << 8) | (keyBytes[i * 4 + 3] & 0xFF);
            }
            return key;
        }
    }

    /**
     * Метод для дешифрования, на вход приходит сообщение для шифрования и ключ
     *
     * @param message
     * @param key
     * @return
     */
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

    /**
     * Метод для дешифрования, на вход приходит зашифрованное тело и ключ
     *
     * @param cipher
     * @param key
     * @return
     */
    private static byte[] teaDecrypt(byte[] cipher, int[] key) {
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

    /**
     * Метод для шифрования внутри файла, на вход подаются пути к файлам. Из одного файла читается, в другой файл записывается с использованием ключа
     *
     * @param mkd_inputFileName
     * @param mkd_outputFileName
     * @param mkd_keyFileName
     * @throws IOException
     */
    private static void mkd_encryptFile(String mkd_inputFileName, String mkd_outputFileName, String mkd_keyFileName) throws IOException {
        int[] key = readKey(mkd_keyFileName);
        try (FileInputStream inputStream = new FileInputStream(mkd_inputFileName);
             FileOutputStream outputStream = new FileOutputStream(mkd_outputFileName)) {
            byte[] inputBytes = inputStream.readAllBytes();
            byte[] encryptedBytes = teaEncrypt(inputBytes, key);
            outputStream.write(encryptedBytes);
        }
    }

    /**
     * Метод для дешифрования внутри файла, на вход подаются пути к файлам. Из одного файла читается, в другой файл записывается с использованием ключа
     *
     * @param mkd_inputFileName
     * @param mkd_outputFileName
     * @param mkd_keyFileName
     * @throws IOException
     */
    private static void mkd_decryptFile(String mkd_inputFileName, String mkd_outputFileName, String mkd_keyFileName) throws IOException {
        int[] key = readKey(mkd_keyFileName);
        try (FileInputStream inputStream = new FileInputStream(mkd_inputFileName);
             FileOutputStream outputStream = new FileOutputStream(mkd_outputFileName)) {
            byte[] encryptedBytes = inputStream.readAllBytes();
            byte[] decryptedBytes = teaDecrypt(encryptedBytes, key);
            outputStream.write(decryptedBytes);
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Введите mode (e для шифрования, d для дешифрования): ");
        String mode = scanner.nextLine();

        System.out.println("Введите имя файла: ");
        String inputFileName = "C:\\Users\\ADMIN\\Desktop\\ИБ\\ЛР1\\" + scanner.nextLine();

        String outputFileName;

        if (mode.equals("e")) {
            outputFileName = inputFileName + ".enc";
        } else if (mode.equals("d")) {
            outputFileName = inputFileName.substring(0, inputFileName.length() - 4);
        } else {
            System.out.println("Invalid mode. Use -e for encryption or -d for decryption.");
            return;
        }

        String keyFileName = "C:\\Users\\ADMIN\\Desktop\\ИБ\\ЛР1\\key.txt";

        try {
            if (mode.equals("e")) {
                mkd_encryptFile(inputFileName, outputFileName, keyFileName);
                System.out.println("Успешное шифрование");
            } else {
                mkd_decryptFile(inputFileName, outputFileName, keyFileName);
                System.out.println("Успешное дешифрование");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }

    }
}