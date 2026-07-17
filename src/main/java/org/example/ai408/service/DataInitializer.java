package org.example.ai408.service;

import jakarta.annotation.PostConstruct;
import org.example.ai408.config.AppProperties;
import org.example.ai408.domain.IdGenerator;
import org.example.ai408.domain.QuestionEntity;
import org.example.ai408.domain.UserEntity;
import org.example.ai408.repository.QuestionRepository;
import org.example.ai408.repository.UserRepository;
import org.example.ai408.util.JsonUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DataInitializer {
    private final AppProperties appProperties;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public DataInitializer(
            AppProperties appProperties,
            QuestionRepository questionRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService
    ) {
        this.appProperties = appProperties;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
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
        String seedEmail = appProperties.admin().seedEmail() == null ? "" : appProperties.admin().seedEmail().trim().toLowerCase();
        UserEntity user = (!seedEmail.isBlank()
                ? userRepository.findByEmail(seedEmail)
                : userRepository.findByMobile(appProperties.admin().seedMobile()))
                .orElseGet(() -> {
                    UserEntity entity = new UserEntity();
                    entity.setId(IdGenerator.prefixed("u"));
                    entity.setMobile(seedEmail.isBlank() ? appProperties.admin().seedMobile() : null);
                    entity.setEmail(seedEmail.isBlank() ? null : seedEmail);
                    return entity;
                });
        if (!seedEmail.isBlank()) {
            user.setEmail(seedEmail);
        }
        if (user.getMobile() == null && seedEmail.isBlank()) {
            user.setMobile(appProperties.admin().seedMobile());
        }
        user.setNickname(appProperties.admin().seedNickname());
        user.setAvatarUrl(user.getAvatarUrl() == null ? "" : user.getAvatarUrl());
        user.setRole("admin");
        user.setWrongBookAutoRemoveEnabled(Boolean.TRUE.equals(user.getWrongBookAutoRemoveEnabled()));
        user.setWrongBookAutoRemoveThreshold(user.getWrongBookAutoRemoveThreshold() == null ? 1 : user.getWrongBookAutoRemoveThreshold());
        user.setLastLoginAt(user.getLastLoginAt() == null ? LocalDateTime.now() : user.getLastLoginAt());
        userRepository.save(user);
    }

    private void seedQuestions() {
        for (QuestionEntity question : buildSeedQuestions()) {
            QuestionEntity target = questionRepository.findByQuestionCode(question.getQuestionCode()).orElse(null);
            if (target == null) {
                questionRepository.save(question);
                continue;
            }
            target.setSubjectCode(question.getSubjectCode());
            target.setSubjectName(question.getSubjectName());
            target.setQuestionType(question.getQuestionType());
            target.setTitle(question.getTitle());
            target.setStem(question.getStem());
            target.setStemImageUrl(question.getStemImageUrl());
            target.setOptionsJson(question.getOptionsJson());
            target.setAnswerJson(question.getAnswerJson());
            target.setAnalysis(question.getAnalysis());
            target.setNote(question.getNote());
            target.setTagsJson(question.getTagsJson());
            target.setNewType(question.getNewType());
            target.setStepsJson(question.getStepsJson());
            target.setDifficulty(question.getDifficulty());
            target.setSortNo(question.getSortNo());
            target.setSource(question.getSource());
            questionRepository.save(target);
        }
    }

    private List<QuestionEntity> buildSeedQuestions() {
        return List.of(
                question("DS-001", "DS", "数据结构", "single",
                        "顺序表最突出的优势是什么？",
                        "在常见线性表实现中，顺序表最大的优势是什么？",
                        jsonOptions("插入删除更快", "随机访问方便", "不用预分配空间", "天然适合频繁扩容"),
                        jsonAnswer("B"),
                        "顺序表支持按下标直接访问元素，因此随机访问时间复杂度为 O(1)。",
                        "看到“按下标”“直接定位”通常就该想到顺序存储的访问优势。",
                        jsonTags("单选题", "数据结构"), false, null, 1, 1, "历年真题"),
                question("DS-002", "DS", "数据结构", "multiple",
                        "关于二叉树遍历，下列说法正确的是？",
                        "请选择二叉树遍历规则中正确的描述。",
                        jsonOptions("先序遍历的第一个结点一定是根结点", "中序遍历的第一个结点一定是根结点", "后序遍历的最后一个结点一定是根结点", "层序遍历必须借助栈"),
                        jsonAnswer("A", "C"),
                        "先序遍历最先访问根，后序遍历最后访问根。层序遍历通常借助队列实现。",
                        "遍历题先定位根结点出现的位置，再看辅助数据结构。",
                        jsonTags("多选题", "树"), false, null, 1, 2, "历年真题"),
                question("CO-001", "CO", "计算机组成原理", "single",
                        "Cache 命中率提高时，平均访存时间会怎样变化？",
                        "Cache 命中率提升会对主存访问产生什么影响？",
                        jsonOptions("下降", "上升", "不变", "无法判断"),
                        jsonAnswer("A"),
                        "命中率越高，访问慢速主存的概率越低，因此平均访存时间下降。",
                        "命中率高意味着访问速度更接近 Cache。",
                        jsonTags("单选题", "Cache"), false, null, 1, 1, "历年真题"),
                question("CO-002", "CO", "计算机组成原理", "single",
                        "写回策略的核心特征是什么？",
                        "Cache 写策略中，写回法的核心特点是什么？",
                        jsonOptions("每次写入都立刻写主存", "只对只读数据有效", "替换时才写回主存", "完全不需要 Cache"),
                        jsonAnswer("C"),
                        "写回策略会先更新 Cache，等该块被替换时再把数据写回主存。",
                        "和写直达做对比记忆最稳。",
                        jsonTags("单选题", "Cache"), true, null, 1, 2, "历年真题"),
                question("OS-001", "OS", "操作系统", "single",
                        "时间片轮转更适合哪类系统？",
                        "哪种系统最适合采用时间片轮转调度？",
                        jsonOptions("批处理系统", "分时系统", "硬实时系统", "嵌入式单任务系统"),
                        jsonAnswer("B"),
                        "时间片轮转强调公平性和响应速度，典型用于分时系统。",
                        "抓住“公平”和“响应快”这两个关键词。",
                        jsonTags("单选题", "调度"), false, null, 1, 1, "历年真题"),
                question("OS-002", "OS", "操作系统", "essay",
                        "银行家算法判断安全性的标准步骤是什么？",
                        "请按步骤完成银行家算法的安全性检查。",
                        jsonOptions(),
                        jsonAnswer("Need", "Work", "Finish"),
                        "先计算每个进程的 Need，初始化 Work 和 Finish，再反复寻找满足 Need <= Work 的进程并释放其资源。",
                        "大题要按步骤走，不要跳过 Need 和 Work 的更新。",
                        jsonTags("综合大题", "死锁"), true,
                        jsonSteps("计算每个进程的 Need 向量", "初始化 Work 和 Finish，寻找满足 Need <= Work 的进程", "假设该进程执行完成并释放资源，重复检查直到全部完成"),
                        3, 2, "历年真题"),
                question("CN-001", "CN", "计算机网络", "single",
                        "TCP 的主要特征是什么？",
                        "下列哪项最符合 TCP 的特征？",
                        jsonOptions("可靠传输", "无连接", "不保证有序", "只用于广播"),
                        jsonAnswer("A"),
                        "TCP 面向连接，并提供可靠、有序的数据传输。",
                        "网络题先区分 TCP 和 UDP，再看是否可靠、是否有连接。",
                        jsonTags("单选题", "传输层"), false, null, 1, 1, "历年真题"),
                question("CN-002", "CN", "计算机网络", "multiple",
                        "下列属于应用层协议的是？",
                        "请选择应用层协议。",
                        jsonOptions("DNS", "TCP", "HTTP", "IP"),
                        jsonAnswer("A", "C"),
                        "DNS 和 HTTP 都属于应用层协议，TCP 属于传输层，IP 属于网络层。",
                        "分层题先给协议找楼层。",
                        jsonTags("多选题", "网络分层"), false, null, 1, 2, "历年真题"),
                question("MOCK-001", "MOCK", "整套模拟", "single",
                        "三次握手中第二次报文段的作用是什么？",
                        "TCP 建立连接时，第二次握手主要在确认什么？",
                        jsonOptions("服务端准备关闭连接", "服务端收发能力正常", "客户端缓存已经清空", "路由已经最短"),
                        jsonAnswer("B"),
                        "第二次握手中的 SYN + ACK 用于确认服务端的收发能力正常，同时回应客户端的 SYN。",
                        "三次握手每一步都在确认一侧或双方的通信能力。",
                        jsonTags("单选题", "整卷模拟"), true, null, 2, 1, "模拟题"),
                question("MOCK-002", "MOCK", "整套模拟", "multiple",
                        "408 常见的题型有哪些？",
                        "冲刺阶段需要重点强化哪些题型？",
                        jsonOptions("选择题", "计算题", "综合大题", "证明题"),
                        jsonAnswer("A", "B", "C"),
                        "408 的主体题型通常是选择、计算和综合大题。",
                        "新题型不等于脱离基础，抓主干最重要。",
                        jsonTags("多选题", "整卷模拟"), true, null, 2, 2, "模拟题")
        );
    }

    private void seedTemplate() throws IOException {
        Path template = fileStorageService.templateDir().resolve("ai408-question-template.xlsx");
        TemplateGenerator.writeTemplate(template);
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
        entity.setStemImageUrl(null);
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
