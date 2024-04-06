package lab4;

import java.io.*;
import java.util.Scanner;


// String inputFileName = "C:\\Users\\ADMIN\\Desktop\\ИБ\\ЛР4\\" + scanner.nextLine();


import java.io.*;
import java.util.Scanner;

public class RLECompression {
    private static final int MAX_REPEAT_COUNT = 127;
    private static final int MAX_NON_REPEAT_COUNT = 128;

    public static void compress(String inputFileName, String outputFileName) throws IOException {
        try (DataInputStream inputStream = new DataInputStream(new FileInputStream(inputFileName));
             DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(outputFileName))) {
            int prevByte = -1;
            int count = 0;

            while (true) {
                int currentByte = inputStream.read();
                if (currentByte == -1) break;

                if (currentByte == prevByte && count < MAX_REPEAT_COUNT) {
                    count++;
                } else {
                    writeCount(outputStream, prevByte, count);
                    count = 1;
                    prevByte = currentByte;
                }
            }

            // Запись последнего фрагмента данных, если он не повторяется
            writeCount(outputStream, prevByte, count);
        }
    }


    private static void writeCount(DataOutputStream outputStream, int value, int count) throws IOException {
        if (count == 1) {
            outputStream.write(0);
            outputStream.write(value);
        } else if (count >= 2 && count <= MAX_REPEAT_COUNT) {
            outputStream.write(count);
            outputStream.write(value);
        }
    }

    public static void decompress(String inputFileName) throws IOException {
        String outputFileName = inputFileName.substring(0, inputFileName.lastIndexOf(".")) + "_decompressed.txt";
        try (DataInputStream inputStream = new DataInputStream(new FileInputStream(inputFileName));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName))) {
            while (true) {
                int count = inputStream.read();
                if (count == -1) break;

                if (count == 0) {
                    int value = inputStream.read();
                    writer.write((char) value);
                } else if (count < 0) {
                    int value = inputStream.read();
                    while (count++ < 0) {
                        writer.write((char) value);
                    }
                } else {
                    int value = inputStream.read();
                    while (count-- > 0) {
                        writer.write((char) value);
                    }
                }
            }
        }
        System.out.println("Разархивация завершена. Файл сохранен как: " + outputFileName);
    }



    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Введите операцию: -c (архивация), -d (разархивация)");
        String operation = scanner.nextLine();
        System.out.println("Введите имя файла:");
        String inputFileName = "C:\\Users\\ADMIN\\Desktop\\ИБ\\ЛР4\\" + scanner.nextLine();

        if (operation.equals("-c")) {
            compress(inputFileName, inputFileName + ".arh");
            System.out.println("Архивация завершена.");
        } else if (operation.equals("-d")) {
            decompress(inputFileName);
        } else {
            System.out.println("Некорректная операция. Используйте -c для архивации и -d для разархивации.");
        }

        scanner.close();
    }
}


