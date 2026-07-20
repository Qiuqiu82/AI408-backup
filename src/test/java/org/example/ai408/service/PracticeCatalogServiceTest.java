package org.example.ai408.service;

import org.example.ai408.domain.QuestionEntity;
import org.example.ai408.domain.UserQuestionStateEntity;
import org.example.ai408.dto.CommonDtos;
import org.example.ai408.repository.QuestionRepository;
import org.example.ai408.repository.UserQuestionStateRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PracticeCatalogServiceTest {
    private final QuestionRepository questionRepository = mock(QuestionRepository.class);
    private final UserQuestionStateRepository stateRepository = mock(UserQuestionStateRepository.class);
    private final PracticeCatalogService service = new PracticeCatalogService(questionRepository, stateRepository);

    @Test
    void listsPapersByYearWithUserProgress() {
        QuestionEntity q2024a = question("q1", "DS-2024-01", "CodeBrick 2024年408真题", "[\"线性表\",\"single\"]");
        QuestionEntity q2024b = question("q2", "CN-2024-40", "CodeBrick 2024年408真题", "[\"TCP\"]");
        QuestionEntity q2025 = question("q3", "OS-2025-12", "", "[\"进程管理\"]");
        QuestionEntity regular = question("q4", "DS-001", "内置题库", "[\"树\"]");

        when(questionRepository.findAll()).thenReturn(List.of(q2024a, q2024b, q2025, regular));
        when(stateRepository.findByUserId("u1")).thenReturn(List.of(
                state("q1", "correct", false),
                state("q2", "wrong", true)
        ));

        List<CommonDtos.PracticeScopeDTO> papers = service.listScopes("u1", "paper");

        assertThat(papers).extracting(CommonDtos.PracticeScopeDTO::scopeKey)
                .containsExactly("2025", "2024");
        assertThat(papers.get(1).totalCount()).isEqualTo(2);
        assertThat(papers.get(1).doneCount()).isEqualTo(2);
        assertThat(papers.get(1).wrongCount()).isEqualTo(1);
    }

    @Test
    void exposesKnowledgePointScopesWithoutQuestionTypeTags() {
        QuestionEntity question = question("q1", "DS-2024-01", "CodeBrick 2024年408真题", "[\"线性表\",\"single\"]");
        when(questionRepository.findAll()).thenReturn(List.of(question));
        when(stateRepository.findByUserId("u1")).thenReturn(List.of());

        List<CommonDtos.PracticeScopeDTO> scopes = service.listScopes("u1", "knowledgePoint");

        assertThat(scopes).extracting(CommonDtos.PracticeScopeDTO::scopeKey)
                .containsExactly("线性表");
    }

    private QuestionEntity question(String id, String code, String source, String tagsJson) {
        QuestionEntity question = new QuestionEntity();
        question.setId(id);
        question.setQuestionCode(code);
        question.setSource(source);
        question.setTagsJson(tagsJson);
        return question;
    }

    private UserQuestionStateEntity state(String questionId, String status, boolean inWrongBook) {
        UserQuestionStateEntity state = new UserQuestionStateEntity();
        state.setQuestionId(questionId);
        state.setQuestionStatus(status);
        state.setInWrongBook(inWrongBook);
        return state;
    }
}
