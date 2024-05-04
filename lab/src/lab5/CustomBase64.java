package lab5;

import java.io.*;
import java.util.Scanner;

/**
 * Класс для кодирования и декодирования данных в Base64.
 */
public class CustomBase64 {

    // Строка символов для кодирования в Base64
    private static final String BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    /**
     * Метод для кодирования данных в Base64.
     *
     * @param inputFileName  Имя входного файла для кодирования.
     * @param outputFileName Имя выходного файла для сохранения кодированных данных.
     * @throws IOException Если произошла ошибка ввода-вывода при чтении/записи файлов.
     */
    public static void encode(String inputFileName, String outputFileName) throws IOException {
        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(inputFileName));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName))) {

            byte[] buffer = new byte[3];
            int numBytes;

            // Читаем данные из входного файла блоками по три байта и кодируем их в Base64
            while ((numBytes = inputStream.read(buffer)) != -1) {
                // Если numBytes < 3, дополняем буфер нулями
                if (numBytes < 3) {
                    buffer[numBytes] = 0;
                }

                // Получаем индексы символов Base64 для каждого байта из буфера
                int index1 = (buffer[0] & 0xFC) >>> 2;
                int index2 = ((buffer[0] & 0x03) << 4) | ((buffer[1] & 0xF0) >>> 4);
                int index3 = ((buffer[1] & 0x0F) << 2) | ((buffer[2] & 0xC0) >>> 6);
                int index4 = buffer[2] & 0x3F;

                // Записываем символы Base64 в выходной файл
                writer.write(BASE64_CHARS.charAt(index1));
                writer.write(BASE64_CHARS.charAt(index2));
                writer.write(numBytes >= 2 ? BASE64_CHARS.charAt(index3) : '='); // Добавляем символ '=' если кол-во байтов < 2
                writer.write(numBytes >= 3 ? BASE64_CHARS.charAt(index4) : '='); // Добавляем символ '=' если кол-во байтов < 3
            }
        }
        System.out.println("Кодирование в Base64 завершено. Файл сохранен как: " + outputFileName);
    }

    /**
     * Метод для декодирования данных из Base64.
     *
     * @param inputFileName  Имя входного файла для декодирования.
     * @param outputFileName Имя выходного файла для сохранения декодированных данных.
     * @throws IOException Если произошла ошибка ввода-вывода при чтении/записи файлов.
     */
    public static void decode(String inputFileName, String outputFileName) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFileName));
             BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFileName))) {

            char[] buffer = new char[4];
            byte[] outputBytes = new byte[3];
            int numChars;

            // Читаем данные из входного файла блоками по четыре символа Base64
            while ((numChars = reader.read(buffer)) != -1) {
                if (numChars == 4) {
                    // Получаем индексы символов Base64 для каждого символа из буфера
                    int index1 = BASE64_CHARS.indexOf(buffer[0]);
                    int index2 = BASE64_CHARS.indexOf(buffer[1]);
                    int index3 = BASE64_CHARS.indexOf(buffer[2]);
                    int index4 = BASE64_CHARS.indexOf(buffer[3]);

                    // Получаем байты из символов Base64
                    outputBytes[0] = (byte) ((index1 << 2) | (index2 >>> 4));
                    outputBytes[1] = (byte) (((index2 & 0x0F) << 4) | (index3 >>> 2));
                    outputBytes[2] = (byte) (((index3 & 0x03) << 6) | index4);

                    // Записываем байты в выходной файл
                    outputStream.write(outputBytes, 0, 3);
                } else {
                    // Игнорируем символы '=' в конце файла
                    break;
                }
            }
        }
        System.out.println("Декодирование из Base64 завершено. Файл сохранен как: " + outputFileName);
    }

    /**
     * Основной метод программы.
     *
     * @param args Аргументы командной строки.
     * @throws IOException Если произошла ошибка ввода-вывода при чтении/записи файлов.
     */
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        // Получаем операцию и имя файла от пользователя
        System.out.println("Введите операцию: -e (кодирование), -d (декодирование)");
        String operation = scanner.nextLine();
        System.out.println("Введите имя файла:");
        String inputFileName = scanner.nextLine();

        // Выполняем выбранную операцию
        if (operation.equals("-e")) {
            encode(inputFileName, inputFileName + ".encoded");
        } else if (operation.equals("-d")) {
            decode(inputFileName, inputFileName.replace(".txt.encoded", "(1).txt"));
        } else {
            System.out.println("Некорректная операция. Используйте -e для кодирования и -d для декодирования.");
        }

        scanner.close();
    }
}
