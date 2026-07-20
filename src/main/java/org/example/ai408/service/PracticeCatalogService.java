package org.example.ai408.service;

import org.example.ai408.common.BusinessException;
import org.example.ai408.common.ErrorCode;
import org.example.ai408.domain.QuestionEntity;
import org.example.ai408.domain.UserQuestionStateEntity;
import org.example.ai408.dto.CommonDtos;
import org.example.ai408.repository.QuestionRepository;
import org.example.ai408.repository.UserQuestionStateRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PracticeCatalogService {
    private final QuestionRepository questionRepository;
    private final UserQuestionStateRepository stateRepository;

    public PracticeCatalogService(QuestionRepository questionRepository, UserQuestionStateRepository stateRepository) {
        this.questionRepository = questionRepository;
        this.stateRepository = stateRepository;
    }

    public List<CommonDtos.PracticeScopeDTO> listScopes(String userId, String requestedType) {
        String scopeType = PracticeScopeSupport.normalizeType(requestedType);
        if (scopeType.isBlank() || PracticeScopeSupport.PAPER.equals(scopeType)) {
            return listPapers(userId);
        }
        if (PracticeScopeSupport.KNOWLEDGE_POINT.equals(scopeType)) {
            return listKnowledgePoints(userId);
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST);
    }

    private List<CommonDtos.PracticeScopeDTO> listPapers(String userId) {
        Map<String, List<QuestionEntity>> papers = questionRepository.findAll().stream()
                .filter(question -> PracticeScopeSupport.paperYear(question) != null)
                .collect(Collectors.groupingBy(
                        PracticeScopeSupport::paperYear,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        Map<String, UserQuestionStateEntity> stateMap = loadStateMap(userId);
        return papers.entrySet().stream()
                .map(entry -> toScopeDto(
                        PracticeScopeSupport.PAPER,
                        entry.getKey(),
                        entry.getKey() + "年408真题",
                        entry.getValue(),
                        stateMap
                ))
                .sorted(Comparator.comparingInt(
                        (CommonDtos.PracticeScopeDTO scope) -> Integer.parseInt(scope.scopeKey())
                ).reversed())
                .toList();
    }

    private List<CommonDtos.PracticeScopeDTO> listKnowledgePoints(String userId) {
        Map<String, List<QuestionEntity>> groups = new LinkedHashMap<>();
        for (QuestionEntity question : questionRepository.findAll()) {
            for (String knowledgePoint : PracticeScopeSupport.knowledgePoints(question)) {
                groups.computeIfAbsent(knowledgePoint, ignored -> new ArrayList<>()).add(question);
            }
        }
        Map<String, UserQuestionStateEntity> stateMap = loadStateMap(userId);
        return groups.entrySet().stream()
                .map(entry -> toScopeDto(
                        PracticeScopeSupport.KNOWLEDGE_POINT,
                        entry.getKey(),
                        entry.getKey(),
                        entry.getValue(),
                        stateMap
                ))
                .sorted(Comparator.comparingInt(CommonDtos.PracticeScopeDTO::totalCount).reversed()
                        .thenComparing(CommonDtos.PracticeScopeDTO::scopeName))
                .toList();
    }

    private Map<String, UserQuestionStateEntity> loadStateMap(String userId) {
        return stateRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(UserQuestionStateEntity::getQuestionId, Function.identity(), (a, b) -> a));
    }

    private CommonDtos.PracticeScopeDTO toScopeDto(
            String scopeType,
            String scopeKey,
            String scopeName,
            List<QuestionEntity> questions,
            Map<String, UserQuestionStateEntity> stateMap
    ) {
        int doneCount = 0;
        int wrongCount = 0;
        for (QuestionEntity question : questions) {
            UserQuestionStateEntity state = stateMap.get(question.getId());
            if (state == null) {
                continue;
            }
            if ("correct".equals(state.getQuestionStatus()) || "wrong".equals(state.getQuestionStatus())) {
                doneCount++;
            }
            if (Boolean.TRUE.equals(state.getInWrongBook()) || "wrong".equals(state.getQuestionStatus())) {
                wrongCount++;
            }
        }
        return new CommonDtos.PracticeScopeDTO(scopeType, scopeKey, scopeName, questions.size(), doneCount, wrongCount);
    }
}
