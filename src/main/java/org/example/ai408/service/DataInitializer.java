package org.example.ai408.service;

import org.example.ai408.config.AppProperties;
import org.example.ai408.domain.IdGenerator;
import org.example.ai408.domain.ImportJobEntity;
import org.example.ai408.domain.QuestionEntity;
import org.example.ai408.domain.UserEntity;
import org.example.ai408.repository.ImportJobRepository;
import org.example.ai408.repository.QuestionRepository;
import org.example.ai408.repository.UserRepository;
import org.example.ai408.util.JsonUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DataInitializer {
    private final AppProperties appProperties;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final ImportJobRepository importJobRepository;
    private final FileStorageService fileStorageService;

    public DataInitializer(
            AppProperties appProperties,
            QuestionRepository questionRepository,
            UserRepository userRepository,
            ImportJobRepository importJobRepository,
            FileStorageService fileStorageService
    ) {
        this.appProperties = appProperties;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.importJobRepository = importJobRepository;
        this.fileStorageService = fileStorageService;
    }

    @PostConstruct
    public void ensureFolders() throws IOException {
        Files.createDirectories(fileStorageService.storageDir());
        Files.createDirectories(fileStorageService.templateDir());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() throws IOException {
        seedAdminUser();
        seedQuestions();
        seedTemplate();
    }

    private void seedAdminUser() {
        userRepository.findByMobile(appProperties.admin().seedMobile()).orElseGet(() -> {
            UserEntity user = new UserEntity();
            user.setId(IdGenerator.prefixed("u"));
            user.setMobile(appProperties.admin().seedMobile());
            user.setNickname(appProperties.admin().seedNickname());
            user.setAvatarUrl("");
            user.setRole("admin");
            user.setLastLoginAt(LocalDateTime.now());
            return userRepository.save(user);
        });
    }

    private void seedQuestions() {
        if (questionRepository.count() > 0) {
            return;
        }
        List<QuestionEntity> questions = List.of(
                question("DS-001", "DS", "数据结构", "single", "顺序表的主要优点是什么？", "在常考的数据结构里，顺序表最突出的特点是什么？", jsonOptions("插入删除更快", "随机访问方便", "空间利用率极高", "无需预分配空间"), jsonAnswer("B"), "顺序表支持按下标随机访问，时间复杂度 O(1)。", "顺序表核心优点是随机访问。", jsonTags("单选"), false, null, 1, 1, "历年真题"),
                question("DS-002", "DS", "数据结构", "multiple", "关于二叉树遍历，下列说法正确的是？", "请选择二叉树遍历规则中正确的描述。", jsonOptions("先序遍历的第一个结点一定是根结点", "中序遍历的第一个结点一定是根结点", "后序遍历的最后一个结点一定是根结点", "层序遍历必须借助栈"), jsonAnswer("A", "C"), "先序首结点是根，后序最后结点是根。层序通常借助队列。", "遍历题先看根结点位置，再看辅助结构是栈还是队列。", jsonTags("多选"), false, null, 1, 2, "历年真题"),
                question("CO-001", "CO", "计算机组成原理", "single", "Cache 命中率上升时，平均访问时间如何变化？", "Cache 命中率提升会对主存访问产生什么影响？", jsonOptions("下降", "上升", "不变", "无法判断"), jsonAnswer("A"), "命中率提高，访问慢速主存的概率下降，平均访问时间随之下降。", "命中率越高，越接近 Cache 的速度。", jsonTags("单选"), false, null, 1, 1, "历年真题"),
                question("CO-002", "CO", "计算机组成原理", "single", "写回策略的特点是？", "Cache 写策略中，写回法的核心特征是什么？", jsonOptions("每次写都立刻写主存", "只对只读数据有效", "替换时才写回主存", "一定不需要 Cache"), jsonAnswer("C"), "写回策略会先更新 Cache，块被替换时再写回主存。", "记忆：直写每次写主存，写回替换时再写。", jsonTags("单选"), true, null, 1, 2, "历年真题"),
                question("OS-001", "OS", "操作系统", "single", "时间片轮转更适合哪类系统？", "哪种系统最适合采用时间片轮转调度？", jsonOptions("批处理系统", "分时系统", "实时系统", "嵌入式系统"), jsonAnswer("B"), "时间片轮转强调公平和响应速度，典型用于分时系统。", "抓住“响应速度”和“公平性”。", jsonTags("单选"), false, null, 1, 1, "历年真题"),
                question("OS-002", "OS", "操作系统", "essay", "银行家算法安全性判断的标准步骤是什么？", "请按步骤完成安全性检查，适合大题分步批改。", jsonOptions(), jsonAnswer("Need", "Work", "Finish"), "银行家算法不断用 Work 去匹配 Need，并在释放资源后更新 Work。", "大题模板：先算 Need，初始化 Work 和 Finish，再找可执行进程。", jsonTags("综合大题"), true, jsonSteps("计算每个进程的 Need 向量", "初始化 Work 和 Finish，寻找满足 Need <= Work 的进程", "假设该进程执行完成并释放资源，重复检查直到全部完成"), 3, 1, "历年真题"),
                question("CN-001", "CN", "计算机网络", "single", "TCP 的主要特点是什么？", "下列哪项最符合 TCP 的特征？", jsonOptions("可靠传输", "无连接", "不保证有序", "只用于广播"), jsonAnswer("A"), "TCP 面向连接，提供可靠、有序的数据传输。", "网络题先分 TCP/UDP，再看连接性和可靠性。", jsonTags("单选"), false, null, 1, 1, "历年真题"),
                question("CN-002", "CN", "计算机网络", "multiple", "下列属于应用层协议的是？", "请选择应用层协议。", jsonOptions("DNS", "TCP", "HTTP", "IP"), jsonAnswer("A", "C"), "DNS、HTTP 都属于应用层；TCP 在传输层，IP 在网络层。", "分层题先定位协议归属。", jsonTags("多选"), false, null, 1, 2, "历年真题"),
                question("MOCK-001", "MOCK", "整套模拟", "single", "三次握手中第二次报文段的作用是？", "TCP 建立连接时，第二次握手主要在确认什么？", jsonOptions("服务器关闭连接", "服务器收发能力正常", "客户端缓存已清空", "路由一定最短"), jsonAnswer("B"), "第二次握手的 SYN+ACK 用来确认服务端收发能力正常。", "握手题记住每一步都在确认对方收发能力。", jsonTags("单选"), true, null, 2, 1, "模拟题"),
                question("MOCK-002", "MOCK", "整套模拟", "multiple", "408 中常见的题型有哪些？", "冲刺阶段需要重点强化哪些题型？", jsonOptions("选择题", "计算题", "综合大题", "证明题"), jsonAnswer("A", "B", "C"), "408 主体题型以选择、计算和综合大题为主。", "新题型不等于脱离基础。", jsonTags("多选"), true, null, 2, 2, "模拟题")
        );
        questionRepository.saveAll(questions);
    }

    private void seedTemplate() throws IOException {
        Path template = fileStorageService.templateDir().resolve("ai408-question-template.xlsx");
        if (Files.exists(template)) {
            return;
        }
        try (InputStream inputStream = new ClassPathResource("templates/ai408-question-template.xlsx").getInputStream()) {
            Files.copy(inputStream, template);
        } catch (Exception ignored) {
            // Fallback: create template on the fly if classpath asset is absent.
            TemplateGenerator.writeTemplate(template);
        }
    }

    private QuestionEntity question(
            String questionCode,
            String subjectCode,
            String subjectName,
            String questionType,
            String title,
            String stem,
            String optionsJson,
            String answerJson,
            String analysis,
            String note,
            String tagsJson,
            Boolean newType,
            String stepsJson,
            Integer difficulty,
            Integer sortNo,
            String source
    ) {
        QuestionEntity entity = new QuestionEntity();
        entity.setId(IdGenerator.prefixed("q"));
        entity.setQuestionCode(questionCode);
        entity.setSubjectCode(subjectCode);
        entity.setSubjectName(subjectName);
        entity.setQuestionType(questionType);
        entity.setTitle(title);
        entity.setStem(stem);
        entity.setOptionsJson(optionsJson);
        entity.setAnswerJson(answerJson);
        entity.setAnalysis(analysis);
        entity.setNote(note);
        entity.setTagsJson(tagsJson);
        entity.setNewType(newType);
        entity.setStepsJson(stepsJson);
        entity.setDifficulty(difficulty);
        entity.setSortNo(sortNo);
        entity.setSource(source);
        return entity;
    }

    private String jsonOptions(String... values) {
        return JsonUtils.write(List.of(values));
    }

    private String jsonAnswer(String... values) {
        return JsonUtils.write(List.of(values));
    }

    private String jsonTags(String... values) {
        return JsonUtils.write(List.of(values));
    }

    private String jsonSteps(String... values) {
        return JsonUtils.write(List.of(values));
    }
}
