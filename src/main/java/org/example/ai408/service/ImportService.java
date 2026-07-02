package org.example.ai408.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.ai408.common.BusinessException;
import org.example.ai408.common.ErrorCode;
import org.example.ai408.domain.IdGenerator;
import org.example.ai408.domain.ImportJobEntity;
import org.example.ai408.domain.QuestionEntity;
import org.example.ai408.dto.CommonDtos;
import org.example.ai408.repository.ImportJobRepository;
import org.example.ai408.repository.QuestionRepository;
import org.example.ai408.util.JsonUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class ImportService {
    private final ImportJobRepository importJobRepository;
    private final QuestionRepository questionRepository;
    private final FileStorageService fileStorageService;

    public ImportService(ImportJobRepository importJobRepository, QuestionRepository questionRepository, FileStorageService fileStorageService) {
        this.importJobRepository = importJobRepository;
        this.questionRepository = questionRepository;
        this.fileStorageService = fileStorageService;
    }

    public CommonDtos.TemplateDTO template() {
        return new CommonDtos.TemplateDTO("/templates/ai408-question-template.xlsx", "v1");
    }

    public CommonDtos.ImportJobDTO submitImport(MultipartFile file, String importType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }
        String safeName = sanitizeFileName(file.getOriginalFilename());
        String ext = fileExtension(safeName);
        if (!List.of("xlsx", "csv").contains(ext)) {
            throw new BusinessException(ErrorCode.IMPORT_FORMAT_INVALID);
        }
        String jobId = IdGenerator.prefixed("job");
        String folder = "imports";
        String storedUrl = fileStorageService.storeUpload(file, folder, jobId + "-" + safeName);

        ImportJobEntity job = new ImportJobEntity();
        job.setJobId(jobId);
        job.setStatus("pending");
        job.setImportType(normalizeImportType(importType));
        job.setTotalCount(0);
        job.setSuccessCount(0);
        job.setFailedCount(0);
        job.setErrorFileUrl(null);
        job.setMessage("queued");
        importJobRepository.save(job);

        CompletableFuture.runAsync(() -> processImport(jobId, storedUrl, normalizeImportType(importType)));
        return Support.toImportJobDto(job);
    }

    public CommonDtos.ImportJobDTO getJob(String jobId) {
        ImportJobEntity job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMPORT_JOB_NOT_FOUND));
        return Support.toImportJobDto(job);
    }

    private void processImport(String jobId, String storedUrl, String importType) {
        ImportJobEntity job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMPORT_JOB_NOT_FOUND));
        job.setStatus("running");
        job.setMessage("processing");
        importJobRepository.save(job);

        List<RowData> rows;
        try {
            rows = readRows(storedUrl);
        } catch (Exception exception) {
            job.setStatus("failed");
            job.setMessage(exception.getMessage());
            importJobRepository.save(job);
            return;
        }

        List<QuestionEntity> validQuestions = new ArrayList<>();
        List<ImportError> errors = new ArrayList<>();

        for (RowData row : rows) {
            try {
                validQuestions.add(parseQuestion(row));
            } catch (Exception exception) {
                errors.add(new ImportError(row.rowNo(), row.value("question_code"), exception.getMessage()));
            }
        }

        if (!validQuestions.isEmpty()) {
            for (QuestionEntity question : validQuestions) {
                questionRepository.findByQuestionCode(question.getQuestionCode())
                        .ifPresentOrElse(existing -> copyQuestion(existing, question), () -> questionRepository.save(question));
            }
            questionRepository.flush();
        }

        String errorFileUrl = errors.isEmpty() ? null : writeErrorFile(jobId, errors);
        job.setTotalCount(rows.size());
        job.setSuccessCount(validQuestions.size());
        job.setFailedCount(errors.size());
        job.setErrorFileUrl(errorFileUrl);
        job.setStatus(validQuestions.isEmpty() && !errors.isEmpty() ? "failed" : "success");
        job.setMessage(String.format(Locale.ROOT, "success=%d, failed=%d, importType=%s", validQuestions.size(), errors.size(), importType));
        importJobRepository.save(job);
    }

    private void copyQuestion(QuestionEntity target, QuestionEntity source) {
        target.setSubjectCode(source.getSubjectCode());
        target.setSubjectName(source.getSubjectName());
        target.setQuestionType(source.getQuestionType());
        target.setTitle(source.getTitle());
        target.setStem(source.getStem());
        target.setOptionsJson(source.getOptionsJson());
        target.setAnswerJson(source.getAnswerJson());
        target.setAnalysis(source.getAnalysis());
        target.setNote(source.getNote());
        target.setTagsJson(source.getTagsJson());
        target.setNewType(source.getNewType());
        target.setStepsJson(source.getStepsJson());
        target.setDifficulty(source.getDifficulty());
        target.setSortNo(source.getSortNo());
        target.setSource(source.getSource());
        questionRepository.save(target);
    }

    private QuestionEntity parseQuestion(RowData row) {
        String questionCode = required(row.value("question_code"), "question_code");
        String subjectCode = required(row.value("subject_code"), "subject_code");
        String questionType = required(row.value("question_type"), "question_type");
        String title = required(row.value("title"), "title");
        String stem = required(row.value("stem"), "stem");
        SubjectCatalog.SubjectMeta meta = SubjectCatalog.get(subjectCode);
        if (meta == null) {
            throw new IllegalArgumentException("unknown subject_code");
        }
        List<String> options = parseJsonList(firstNonBlank(row, "options_json", "options"));
        List<String> answer = parseJsonList(firstNonBlank(row, "answer_json", "answer"));
        List<String> tags = parseJsonList(firstNonBlank(row, "tags", "tags_json"));
        List<String> steps = parseJsonList(firstNonBlank(row, "steps_json", "steps"));
        if (("single".equals(questionType) || "multiple".equals(questionType)) && options.size() < 2) {
            throw new IllegalArgumentException("options_json requires at least 2 options");
        }
        if (answer.isEmpty()) {
            throw new IllegalArgumentException("answer_json is required");
        }
        if ("single".equals(questionType) && answer.size() != 1) {
            throw new IllegalArgumentException("single question answer size must be 1");
        }
        if ("essay".equals(questionType) && steps.isEmpty()) {
            throw new IllegalArgumentException("essay question steps_json is required");
        }

        QuestionEntity question = new QuestionEntity();
        question.setId(IdGenerator.prefixed("q"));
        question.setQuestionCode(questionCode);
        question.setSubjectCode(subjectCode);
        question.setSubjectName(meta.subjectName());
        question.setQuestionType(questionType);
        question.setTitle(title);
        question.setStem(stem);
        question.setOptionsJson(JsonUtils.write(options));
        question.setAnswerJson(JsonUtils.write(answer));
        question.setAnalysis(firstNonBlank(row, "analysis"));
        question.setNote(firstNonBlank(row, "note"));
        question.setTagsJson(JsonUtils.write(tags.isEmpty() ? List.of(questionType) : tags));
        question.setNewType(parseBoolean(firstNonBlank(row, "new_type", "newType")));
        question.setStepsJson(JsonUtils.write(steps));
        question.setDifficulty(parseInteger(firstNonBlank(row, "difficulty")));
        question.setSortNo(parseInteger(firstNonBlank(row, "sort_no", "sortNo")));
        question.setSource(firstNonBlank(row, "source"));
        return question;
    }

    private List<RowData> readRows(String storedUrl) throws IOException {
        String relative = storedUrl.startsWith("/files/") ? storedUrl.substring("/files/".length()) : storedUrl;
        Path file = fileStorageService.resolveStorage(relative);
        String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".csv")) {
            return readCsv(file);
        }
        if (fileName.endsWith(".xlsx")) {
            return readExcel(file);
        }
        throw new BusinessException(ErrorCode.IMPORT_FORMAT_INVALID);
    }

    private List<RowData> readCsv(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {
            List<RowData> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                Map<String, String> values = record.toMap().entrySet().stream()
                        .collect(Collectors.toMap(entry -> normalizeHeader(entry.getKey()), entry -> entry.getValue() == null ? "" : entry.getValue(), (a, b) -> a, LinkedHashMap::new));
                rows.add(new RowData((int) record.getRecordNumber() + 1, values));
            }
            return rows;
        }
    }

    private List<RowData> readExcel(Path file) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            Map<Integer, String> headers = new LinkedHashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                headers.put(i, normalizeHeader(formatter.formatCellValue(headerRow.getCell(i))));
            }
            List<RowData> rows = new ArrayList<>();
            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                Map<String, String> values = new LinkedHashMap<>();
                boolean anyValue = false;
                for (int c = 0; c < headers.size(); c++) {
                    String value = formatter.formatCellValue(row.getCell(c));
                    if (value != null && !value.isBlank()) {
                        anyValue = true;
                    }
                    values.put(headers.get(c), value == null ? "" : value.trim());
                }
                if (anyValue) {
                    rows.add(new RowData(i + 1, values));
                }
            }
            return rows;
        }
    }

    private String writeErrorFile(String jobId, List<ImportError> errors) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("errors");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("row_no");
            header.createCell(1).setCellValue("question_code");
            header.createCell(2).setCellValue("error_message");
            for (int i = 0; i < errors.size(); i++) {
                Row row = sheet.createRow(i + 1);
                ImportError error = errors.get(i);
                row.createCell(0).setCellValue(error.rowNo());
                row.createCell(1).setCellValue(error.questionCode() == null ? "" : error.questionCode());
                row.createCell(2).setCellValue(error.message());
            }
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            workbook.write(outputStream);
            return fileStorageService.storeBytes(outputStream.toByteArray(), "import-errors", "import-error-" + jobId + ".xlsx");
        } catch (IOException e) {
            throw new IllegalStateException("write error file failed", e);
        }
    }

    private String normalizeImportType(String importType) {
        if (importType == null || importType.isBlank()) {
            return "append";
        }
        return importType.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeHeader(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
    }

    private String firstNonBlank(RowData row, String... keys) {
        for (String key : keys) {
            String value = row.value(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private List<String> parseJsonList(String value) {
        if (value == null || value.isBlank()) {
            return new ArrayList<>();
        }
        if (value.startsWith("[") && value.endsWith("]")) {
            return JsonUtils.readStringList(value);
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private boolean parseBoolean(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return List.of("1", "true", "yes", "y", "是").contains(normalized);
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String sanitizeFileName(String value) {
        if (value == null || value.isBlank()) {
            return "questions.xlsx";
        }
        String fileName = Path.of(value).getFileName().toString();
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String fileExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private record RowData(int rowNo, Map<String, String> values) {
        String value(String key) {
            return values.get(normalizeHeader(key));
        }
    }

    private record ImportError(int rowNo, String questionCode, String message) {
    }
}
