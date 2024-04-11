package lab4;
import java.io.*;
import java.util.*;

import java.io.*;
import java.util.*;

public class LZWCompression {
    // Максимальное количество повторений для архивации
    private static final int MAX_REPEAT_COUNT = 127;

    // Метод для архивации файла с использованием алгоритма LZW
    public static void compress(String inputFileName, String outputFileName) throws IOException {
        // Создаем словарь для кодирования
        Map<String, Integer> dictionary = new HashMap<>();
        for (int i = 0; i < 256; i++) { // Заполняем словарь символами ASCII
            dictionary.put("" + (char)i, i);
        }

        // Читаем входной файл и записываем закодированные данные в выходной файл
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFileName));
             DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(outputFileName))) {
            StringBuilder currentSequence = new StringBuilder(); // Текущая последовательность
            int code = 256; // Код для следующей последовательности

            int currentChar;
            while ((currentChar = reader.read()) != -1) {
                char currentSymbol = (char)currentChar; // Текущий символ из входного файла
                String sequencePlusSymbol = currentSequence.toString() + currentSymbol; // Текущая последовательность с добавленным символом
                if (dictionary.containsKey(sequencePlusSymbol)) {
                    currentSequence.append(currentSymbol); // Если последовательность уже есть в словаре, добавляем следующий символ
                } else {
                    outputStream.writeInt(dictionary.get(currentSequence.toString())); // Записываем код текущей последовательности

                    // Добавляем новую последовательность в словарь
                    if (code < 65536) { // Максимальное значение для двух байт
                        dictionary.put(sequencePlusSymbol, code++);
                    }
                    currentSequence = new StringBuilder("" + currentSymbol); // Начинаем новую последовательность с текущего символа
                }
            }
            if (!currentSequence.toString().equals("")) { // Записываем оставшуюся последовательность
                outputStream.writeInt(dictionary.get(currentSequence.toString()));
            }
        }
    }

    // Метод для разархивации файла с использованием алгоритма LZW
    public static void decompress(String inputFileName) throws IOException {
        // Создаем словарь для декодирования
        List<String> dictionary = new ArrayList<>();
        for (int i = 0; i < 256; i++) { // Заполняем словарь символами ASCII
            dictionary.add("" + (char)i);
        }

        // Читаем входной файл и записываем декодированные данные в выходной файл
        String outputFileName = inputFileName.replace(".txt.lzw", "(1).txt");
        try (DataInputStream inputStream = new DataInputStream(new FileInputStream(inputFileName));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName))) {
            int previousCode = inputStream.readInt(); // Код предыдущей последовательности
            writer.write(dictionary.get(previousCode)); // Записываем первую последовательность

            int currentCode;
            while (inputStream.available() > 0) {
                currentCode = inputStream.readInt(); // Читаем следующий код
                String currentSequence;
                if (currentCode < dictionary.size()) {
                    currentSequence = dictionary.get(currentCode); // Получаем последовательность из словаря
                } else if (currentCode == dictionary.size()) {
                    currentSequence = dictionary.get(previousCode) + dictionary.get(previousCode).charAt(0); // Получаем новую последовательность
                } else {
                    throw new IllegalArgumentException("Неправильный код при декодировании: " + currentCode); // Обработка ошибки
                }
                writer.write(currentSequence); // Записываем текущую последовательность

                if (dictionary.size() < 65536) { // Максимальное значение для двух байт
                    dictionary.add(dictionary.get(previousCode) + currentSequence.charAt(0)); // Добавляем новую последовательность в словарь
                }
                previousCode = currentCode; // Обновляем предыдущий код
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
        String inputFileName = "C:\\Users\\ADMIN\\Desktop\\ИБ\\ЛР4\\LZW\\" + scanner.nextLine();

        // Выполняем выбранную операцию
        if (operation.equals("-c")) {
            compress(inputFileName, inputFileName + ".lzw");
            System.out.println("Архивация завершена.");
        } else if (operation.equals("-d")) {
            decompress(inputFileName);
        } else {
            System.out.println("Некорректная операция. Используйте -c для архивации и -d для разархивации.");
        }

        // Закрываем сканнер
        scanner.close();
    }
}

