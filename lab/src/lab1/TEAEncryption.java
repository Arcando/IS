package lab1;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Scanner;

public class TEAEncryption {
    // Константа DELTA для TEA
    private static final int DELTA = 0x9e3779b9;
    // Количество раундов для TEA
    private static final int NUM_ROUNDS = 32;
    // Размер блока данных для TEA (в байтах)
    private static final int BLOCK_SIZE = 8; // 64 бита

    /**
     * Генерация ключа шифрования и запись в файл
     *
     * @param keyFileName Имя файла для хранения ключа
     * @throws IOException Ошибка ввода/вывода
     */
    private static void generateKey(String keyFileName) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(keyFileName)) {
            SecureRandom secureRandom = new SecureRandom();
            // Генерация ключа длиной 16 байт
            byte[] key = new byte[BLOCK_SIZE * 2];
            secureRandom.nextBytes(key);
            outputStream.write(key);
        }
    }

    /**
     * Чтение ключа из файла
     *
     * @param keyFileName Имя файла с ключом
     * @return Массив int, содержащий ключ
     * @throws IOException Ошибка ввода/вывода
     */
    private static int[] readKey(String keyFileName) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(keyFileName)) {
            byte[] keyBytes = inputStream.readAllBytes();
            // Проверка длины ключа
            if (keyBytes.length % 4 != 0) {
                throw new IllegalArgumentException("Некорректная длина ключа");
            }
            int[] key = new int[keyBytes.length / 4];
            // Преобразование массива байт в массив int
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
     * Шифрование данных методом TEA
     *
     * @param message Исходный блок данных
     * @param key     Ключ шифрования
     * @return Зашифрованный блок данных
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
     * Шифрование файла
     *
     * @param inputFileName  Входной файл для шифрования
     * @param outputFileName Выходной файл с зашифрованными данными
     * @param keyFileName    Файл с ключом шифрования
     * @throws IOException Ошибка ввода/вывода
     */
    private static void encryptFile(String inputFileName, String outputFileName, String keyFileName) throws IOException {
        int[] key = readKey(keyFileName);

        try (FileInputStream inputStream = new FileInputStream(inputFileName);
             FileOutputStream outputStream = new FileOutputStream(outputFileName)) {
            byte[] inputBytes = inputStream.readAllBytes();
            if (inputBytes.length != BLOCK_SIZE) {
                throw new ArrayIndexOutOfBoundsException("Размер блока данных должен быть равен " + BLOCK_SIZE + " байт");
            }
            byte[] encryptedBytes = teaEncrypt(inputBytes, key);
            outputStream.write(encryptedBytes);
        }
    }

    /**
     * Дешифрование файла
     *
     * @param inputFileName Имя зашифрованного файла
     * @param keyFileName   Файл с ключом шифрования
     * @throws IOException Ошибка ввода/вывода
     */
    private static void decryptFile(String inputFileName, String keyFileName) throws IOException {
        int[] key = readKey(keyFileName);
        try (FileInputStream inputStream = new FileInputStream(inputFileName);
             FileOutputStream outputStream = new FileOutputStream(inputFileName.replace(".txt.enc", "decrypt.txt"))) {
            byte[] encryptedBytes = inputStream.readAllBytes();
            byte[] decryptedBytes = teaDecrypt(encryptedBytes, key);
            outputStream.write(decryptedBytes);
        }
    }

    /**
     * Дешифрование данных методом TEA
     *
     * @param cipher Зашифрованный блок данных
     * @param key    Ключ шифрования
     * @return Расшифрованный блок данных
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

    public static void main(String[] args) throws IOException {
        // Пути к файлам
        String messageFileName = "C:\\Users\\ADMIN\\Desktop\\ИБ\\ЛР1\\message.txt"; // Путь к файлу с сообщением для шифрования
        String keyFileName = "C:\\Users\\ADMIN\\Desktop\\ИБ\\ЛР1\\key.txt"; // Путь к файлу с ключем для шифрования

        // Ввод режима работы (шифрование или дешифрование) с консоли
        Scanner scanner = new Scanner(System.in);
        System.out.println("Введите тип операции: -e - шифрование, -d - дешифрование");
        String mode = scanner.nextLine();

        if (mode.equalsIgnoreCase("-e")) {
            // Генерация ключа шифрования
            generateKey(keyFileName);
            // Шифрование файла
            encryptFile(messageFileName, messageFileName + ".enc", keyFileName);
            System.out.println("Шифрование завершено.");
        } else if (mode.equalsIgnoreCase("-d")) {
            // Дешифрование файла
            decryptFile(messageFileName + ".enc", keyFileName);
            System.out.println("Дешифрование завершено.");
        }
        else {
            // Некорректный режим работы
            throw new IOException("Некорректный режим работы.");
        }
    }
}

