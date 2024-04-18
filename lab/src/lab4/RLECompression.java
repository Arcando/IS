package lab4;

import java.io.*;
import java.util.Scanner;

public class RLECompression {
    private static final int MIN_REPEAT_COUNT = 3; // Минимальное количество повторов одного символа

    // Метод для архивации файла с использованием алгоритма RLE
    public static void compress(String inputFileName, String outputFileName) throws IOException {
        // Открываем входной и выходной потоки данных для чтения и записи файлов
        try (DataInputStream inputStream = new DataInputStream(new FileInputStream(inputFileName));
             DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(outputFileName))) {
            int prevByte = -1; // Предыдущий считанный байт
            int count = 1; // Счетчик повторений символов

            // Читаем входной файл побайтно и выполняем архивацию
            while (true) {
                int currentByte = inputStream.read();
                if (currentByte == -1) break; // Проверка на конец файла

                // Если текущий байт совпадает с предыдущим
                if (currentByte == prevByte) {
                    count++; // Увеличиваем счетчик повторений
                } else {
                    writeCount(outputStream, prevByte, count); // Записываем данные в выходной файл
                    count = 1; // Сбрасываем счетчик повторений
                    prevByte = currentByte; // Обновляем предыдущий байт
                }
            }

            // Запись последнего фрагмента данных
            writeCount(outputStream, prevByte, count);
        }
    }

    // Метод для записи данных в выходной файл
    private static void writeCount(DataOutputStream outputStream, int value, int count) throws IOException {
        if (count >= MIN_REPEAT_COUNT) { // Если символ повторяется 3 и более раз
            outputStream.write(0); // Записываем ноль для обозначения повторяющейся последовательности
            outputStream.write(count); // Записываем количество повторений
            outputStream.write(value); // Записываем символ
        } else { // Если символ повторяется менее 3 раз
            for (int i = 0; i < count; i++) {
                outputStream.write(1); // Записываем единицу для обозначения отдельного символа
                outputStream.write(value); // Записываем символ
            }
        }
    }

    // Метод для разархивации файла
    public static void decompress(String inputFileName) throws IOException {
        // Создаем имя выходного файла на основе имени входного файла
        String outputFileName = inputFileName.replace(".arh", "(1).txt");

        // Открываем входной поток данных для чтения архивированного файла и выходной поток данных для записи разархивированного файла
        try (DataInputStream inputStream = new DataInputStream(new FileInputStream(inputFileName));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName))) {
            // Читаем данные из архивированного файла и производим их разархивацию
            while (true) {
                int flag = inputStream.read(); // Читаем флаг (0 для повторяющейся последовательности, 1 для отдельного символа)
                if (flag == -1) break; // Проверка на конец файла

                if (flag == 0) { // Если символ повторяется
                    int count = inputStream.read(); // Читаем количество повторений
                    int value = inputStream.read(); // Читаем символ
                    for (int i = 0; i < count; i++) {
                        writer.write((char) value); // Записываем символ в выходной файл
                    }
                } else { // Если символ не повторяется
                    int value = inputStream.read(); // Читаем символ
                    writer.write((char) value); // Записываем символ в выходной файл
                }
            }
        }

        // Выводим сообщение о завершении разархивации
        System.out.println("Разархивация завершена. Файл сохранен как: " + outputFileName);
    }

    // Основной метод программы
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        // Получаем операцию и имя файла от пользователя
        System.out.println("Введите операцию: -c (архивация), -d (разархивация)");
        String operation = scanner.nextLine();
        System.out.println("Введите имя файла:");
        String inputFileName = "C:\\Users\\ADMIN\\Desktop\\ИБ\\ЛР4\\RLE\\" + scanner.nextLine();

        // Выполняем выбранную операцию
        if (operation.equals("-c")) {
            compress(inputFileName, inputFileName + ".arh");
            System.out.println("Архивация завершена.");
        } else if (operation.equals("-d")) {
            decompress(inputFileName);
        } else {
            System.out.println("Некорректная операция. Используйте -c для архивации и -d для разархивации.");
        }
    }
}

