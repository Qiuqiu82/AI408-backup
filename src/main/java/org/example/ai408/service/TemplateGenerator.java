package org.example.ai408.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
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
            CellStyle style = workbook.createCellStyle();
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
            }
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("DS-1001");
            sample.createCell(1).setCellValue("DS");
            sample.createCell(2).setCellValue("single");
            sample.createCell(3).setCellValue("示例题目");
            sample.createCell(4).setCellValue("这里填写题干");
            sample.createCell(5).setCellValue("[\"A\",\"B\"]");
            sample.createCell(6).setCellValue("[\"A\"]");
            sample.createCell(7).setCellValue("这里填写解析");
            sample.createCell(8).setCellValue("备注");
            sample.createCell(9).setCellValue("[\"单选\"]");
            sample.createCell(10).setCellValue("0");
            sample.createCell(11).setCellValue("[]");
            sample.createCell(12).setCellValue("1");
            sample.createCell(13).setCellValue("1");
            sample.createCell(14).setCellValue("手动导入");
            workbook.write(outputStream);
        }
    }
}
