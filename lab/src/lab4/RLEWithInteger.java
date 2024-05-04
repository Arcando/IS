package lab4;

import java.io.*;

public class RLEWithInteger {
    private static final int MIN_REPEAT_COUNT = 3; // Минимальное количество повторов одного символа

    public static void compress(String inputFileName, String outputFileName) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFileName));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int count = line.length(); // Подсчитываем количество символов в строке
                if (hasRepeatedCharacters(line) && count >= MIN_REPEAT_COUNT) {
                    writer.write(Integer.toString(count)); // Записываем длину строки
                    writer.write(line.charAt(0)); // Записываем первый символ строки
                    writer.write(line.substring(1)); // Записываем остальную часть строки
                    writer.newLine(); // Добавляем новую строку
                } else {
                    writer.write(Integer.toString(count)); // Записываем длину строки
                    writer.write(line); // Записываем саму строку
                    writer.newLine(); // Добавляем новую строку
                }
            }
        }
    }

    public static void decompress(String inputFileName) throws IOException {
        // Создаем имя выходного файла на основе имени входного файла
        String outputFileName = inputFileName.replace(".txt.arh", "(1).txt");

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFileName));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Пропускаем строки, начинающиеся с числа
                if (Character.isDigit(line.charAt(0))) {
                    continue;
                }
                writer.write(line); // Записываем строку
                writer.newLine(); // Добавляем новую строку
            }
        }
    }

    // Метод для проверки наличия повторяющихся символов в строке
    private static boolean hasRepeatedCharacters(String str) {
        for (int i = 0; i < str.length() - 1; i++) {
            if (str.charAt(i) == str.charAt(i + 1)) {
                return true;
            }
        }
        return false;
    }

    // Основной метод программы
    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Введите операцию: -c (архивация), -d (разархивация)");
        String operation = br.readLine();
        System.out.println("Введите имя файла:");
        String inputFileName = "C:\\Users\\ADMIN\\Desktop\\ИБ\\ЛР4\\RLE\\" + br.readLine();

        if (operation.equals("-c")) {
            compress(inputFileName, inputFileName + ".arh");
            System.out.println("Архивация завершена.");
        } else if (operation.equals("-d")) {
            decompress(inputFileName);
            System.out.println("Разархивация завершена. Файл сохранен как: " + inputFileName);
        } else {
            System.out.println("Некорректная операция. Используйте -c для архивации и -d для разархивации.");
        }
    }
}

