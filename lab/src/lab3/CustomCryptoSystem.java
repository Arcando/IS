package lab3;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Scanner;

public class CustomCryptoSystem {
    private static final int SESSION_KEY_SIZE = 16; // Размер ключа сеанса в байтах

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        // Шаг 1: Генерация ключа сеанса
        byte[] sessionKey = generateSessionKey();
        System.out.println("Сгенерирован ключ сеанса.");

//        // Ввод пароля пользователя без вывода на экран
//        Console console = System.console();
//        if (console == null) {
//            System.out.println("Консоль не доступна. Пароль будет виден на экране.");
//        }
        System.out.println("Введите пароль: ");
        String password = scanner.nextLine();

        // Шаг 5: Создание ключа шифрования на основе пароля пользователя
        byte[] encryptionKey = generateEncryptionKey(password);

        // Шаг 3: Шифрование файла с использованием ключа сеанса
        String inputFileName = "C:\\Users\\ADMIN\\Desktop\\ИБ\\ЛР5\\lab5.txt";
        String encryptedFileName = encryptFile(inputFileName, sessionKey);

        // Шаг 7: Шифрование ключа сеанса и добавление его в зашифрованный файл
        encryptSessionKey(encryptedFileName, sessionKey, encryptionKey);

        // Дешифрование файла
        decryptFile(encryptedFileName, encryptionKey);

        System.out.println("Программа завершена.");
    }

    // Генерация ключа сеанса
    private static byte[] generateSessionKey() throws NoSuchAlgorithmException {
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[SESSION_KEY_SIZE];
        secureRandom.nextBytes(key);
        return key;
    }

    // Создание ключа шифрования на основе пароля пользователя
    private static byte[] generateEncryptionKey(String password) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(password.getBytes());
    }

    // Шифрование файла с использованием ключа сеанса
    private static String encryptFile(String inputFileName, byte[] sessionKey) throws Exception {
        String outputFileName = inputFileName + ".enc";
        try (FileInputStream inputStream = new FileInputStream(inputFileName);
             FileOutputStream outputStream = new FileOutputStream(outputFileName)) {
            Cipher cipher = Cipher.getInstance("AES");
            SecretKeySpec secretKeySpec = new SecretKeySpec(sessionKey, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher);

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                cipherOutputStream.write(buffer, 0, bytesRead);
            }
            cipherOutputStream.close();
        }
        return outputFileName;
    }

    // Шифрование ключа сеанса и добавление его в зашифрованный файл
    private static void encryptSessionKey(String encryptedFileName, byte[] sessionKey, byte[] encryptionKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec secretKeySpec = new SecretKeySpec(encryptionKey, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

        try (FileOutputStream outputStream = new FileOutputStream(encryptedFileName, true)) {
            byte[] encryptedSessionKey = cipher.doFinal(sessionKey);
            outputStream.write(encryptedSessionKey);
        }
    }

    // Дешифрование файла
    private static void decryptFile(String encryptedFileName, byte[] encryptionKey) throws Exception {
        // Считываем зашифрованный ключ сеанса
        int sessionKeySize = SESSION_KEY_SIZE;
        byte[] encryptedSessionKey = new byte[sessionKeySize];
        try (RandomAccessFile file = new RandomAccessFile(encryptedFileName, "r")) {
            long fileLength = file.length();
            file.seek(fileLength - sessionKeySize);
            file.read(encryptedSessionKey);
        }

        // Расшифровываем зашифрованный ключ сеанса
        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec secretKeySpec = new SecretKeySpec(encryptionKey, "AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        byte[] sessionKey = cipher.doFinal(encryptedSessionKey);

        // Проверяем, что sessionKey не пустой
        if (sessionKey == null || sessionKey.length == 0) {
            throw new IllegalArgumentException("Empty session key");
        }

        // Расшифровываем файл с использованием расшифрованного ключа сеанса
        try (FileInputStream inputStream = new FileInputStream(encryptedFileName);
             FileOutputStream outputStream = new FileOutputStream(encryptedFileName.replace(".enc", ""))) {
            Cipher decryptCipher = Cipher.getInstance("AES");
            SecretKeySpec sessionKeySpec = new SecretKeySpec(sessionKey, "AES");
            decryptCipher.init(Cipher.DECRYPT_MODE, sessionKeySpec);
            CipherInputStream cipherInputStream = new CipherInputStream(inputStream, decryptCipher);

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = cipherInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            cipherInputStream.close();
        }
    }


}
