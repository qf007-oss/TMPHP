import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MainTestTarget {

    public static void main(String[] args) {
        String inputFilePath = "output.txt";  // 输入文件路径
        String outputFilePath = "target.txt";  // 输出文件路径
        String[] targetNumbers = {"16967"};  // 目标数字数组

        // 调用方法处理文件
        int count = filterLinesByAllTargets(inputFilePath, outputFilePath, targetNumbers);

        // 打印输出文件中的记录数
        System.out.println("符合条件的记录数: " + count);
    }

    /**
     * 根据包含所有目标数字过滤文件中的行，并将符合条件的行输出到另一个文件中。
     *
     * @param inputFilePath  输入文件路径
     * @param outputFilePath 输出文件路径
     * @param targetNumbers  目标数字数组
     * @return 输出文件中的记录数
     */
    public static int filterLinesByAllTargets(String inputFilePath, String outputFilePath, String[] targetNumbers) {
        List<String> resultLines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" "); // 先按空格分割行
                boolean containsAllTargets = true;

                // 只检查 #UTIL: 前面的部分
                for (String target : targetNumbers) {
                    boolean found = false;
                    for (String part : parts) {
                        if (part.equals("#UTIL:")) {
                            break;  // 只检查到 #UTIL: 为止
                        }
                        if (part.equals(target)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        containsAllTargets = false;
                        break;
                    }
                }

                if (containsAllTargets) {
                    resultLines.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 将结果写入输出文件
        int count = 0;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            for (String resultLine : resultLines) {
                writer.write(resultLine);
                writer.newLine();
                count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return count;  // 返回输出文件中的记录数
    }
}

