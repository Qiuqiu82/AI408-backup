package org.example.ai408.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TemplateGenerator {
    private TemplateGenerator() {
    }

    public static void writeTemplate(Path target) throws IOException {
        Files.createDirectories(target.getParent());
        try (Workbook workbook = new XSSFWorkbook(); OutputStream outputStream = Files.newOutputStream(target)) {
            Sheet sheet = workbook.createSheet("questions");
            String[] headers = {
                    "question_code", "subject_code", "question_type", "title", "stem",
                    "options_json", "answer_json", "analysis", "note", "tags",
                    "new_type", "steps_json", "difficulty", "sort_no", "source"
            };

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
            }

            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("DS-1001");
            sample.createCell(1).setCellValue("DS");
            sample.createCell(2).setCellValue("single");
            sample.createCell(3).setCellValue("顺序表最突出的优势是什么？");
            sample.createCell(4).setCellValue("在常见线性表实现中，顺序表最大的优势是什么？");
            sample.createCell(5).setCellValue("[\"插入删除方便\",\"随机访问方便\",\"不用预分配空间\",\"天然适合频繁扩容\"]");
            sample.createCell(6).setCellValue("[\"B\"]");
            sample.createCell(7).setCellValue("顺序表支持按下标直接访问元素，因此随机访问效率高。");
            sample.createCell(8).setCellValue("单选题示例");
            sample.createCell(9).setCellValue("[\"单选题\",\"数据结构\"]");
            sample.createCell(10).setCellValue("0");
            sample.createCell(11).setCellValue("[]");
            sample.createCell(12).setCellValue("1");
            sample.createCell(13).setCellValue("1");
            sample.createCell(14).setCellValue("手动导入");

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
        }
    }
}
