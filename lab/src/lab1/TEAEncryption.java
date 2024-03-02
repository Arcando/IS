package lab1;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Scanner;

public class TEAEncryption {
    private static final int DELTA = 0x9e3779b9;//
    private static final int NUM_ROUNDS = 32;//
    private static final int BLOCK_SIZE = 8; // 64 bits

    /**
     * Открывается файл и заполняется псевдослучайными числами до 16 байт
     *
     * @param keyFileName
     * @throws IOException
     */
    private static void generateKey(String keyFileName) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(keyFileName)) {
            SecureRandom secureRandom = new SecureRandom();
            byte[] key = new byte[BLOCK_SIZE * 2];
            secureRandom.nextBytes(key);
            outputStream.write(key);
        }
    }


    /**
     * Считывается ключ из файла, если размер не равен 16 байт, пробрасывается IllegalArgumentException
     *
     * @param keyFileName
     * @return
     * @throws IOException
     */
    private static int[] readKey(String keyFileName) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(keyFileName)) {
            byte[] keyBytes = inputStream.readAllBytes();
            if (keyBytes.length % 4 != 0) {
                throw new IllegalArgumentException("Не валидная длина ключа");
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


    /**
     * Сообщение кодируется с использованием ключа
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
     * Сообщение декодируется с использованием ключа
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
     * Происходит шифрование внутри файла, с использованием метода teaEncrypt
     * @param inputFileName
     * @param outputFileName
     * @param keyFileName
     * @throws IOException
     */
    private static void mkd_encryptFile(String inputFileName, String outputFileName, String keyFileName) throws IOException {
        int[] key = readKey(keyFileName);

        try (FileInputStream inputStream = new FileInputStream(inputFileName);
             FileOutputStream outputStream = new FileOutputStream(outputFileName)) {
            byte[] inputBytes = inputStream.readAllBytes();
            if (inputBytes.length != 8) {
                throw new ArrayIndexOutOfBoundsException("Тело сообщения должно равняться 8 байт");
            }
            byte[] encryptedBytes = teaEncrypt(inputBytes, key);
            outputStream.write(encryptedBytes);
        }
    }

    /**
     * Происходит дешифрование внутри файла, с использованием метода teaDecrypt
     * @param inputFileName
     * @param keyFileName
     * @throws IOException
     */
    private static void mkd_decryptFile(String inputFileName, String keyFileName) throws IOException {
        int[] key = readKey(keyFileName);
        try (FileInputStream inputStream = new FileInputStream(inputFileName);
             FileOutputStream outputStream = new FileOutputStream(inputFileName.replace(".txt.enc", "decrypt.txt"))) {
            byte[] encryptedBytes = inputStream.readAllBytes();
            byte[] decryptedBytes = teaDecrypt(encryptedBytes, key);
            outputStream.write(decryptedBytes);
        }
    }

    public static void main(String[] args) throws IOException {
        String messageFileName = "C:\\Users\\ADMIN\\Desktop\\ИБ\\ЛР1\\message.txt";//путь к файлу с сообщением для шифрования
        String keyFileName = "C:\\Users\\ADMIN\\Desktop\\ИБ\\ЛР1\\key.txt";//путь к файлу с ключем для шифрования

        Scanner scanner = new Scanner(System.in);//считываем строку с конлоси
        System.out.println("Ввдите тип опперации: -e - шифрование, -d - дешифрование");
        String mode = scanner.nextLine();

        if (mode.equalsIgnoreCase("-e")) {
            generateKey(keyFileName);
            mkd_encryptFile(messageFileName, messageFileName + ".enc", keyFileName);
            System.out.println("Шифрование завершено.");
        } else if (mode.equalsIgnoreCase("-d")) {
            mkd_decryptFile(messageFileName + ".enc", keyFileName);
            System.out.println("Дешифрование завершено.");
        } else throw new IOException();

    }

}
