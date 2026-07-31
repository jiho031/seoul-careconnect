package com.seoulcareconnect.service.user;

import com.seoulcareconnect.dto.user.PolicyDocumentProgressResponse;
import com.seoulcareconnect.entity.policy.PolicyDocument;
import com.seoulcareconnect.entity.user.PolicyDocumentProgress;
import com.seoulcareconnect.repository.policy.PolicyDocumentRepository;
import com.seoulcareconnect.repository.user.PolicyDocumentProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyDocumentProgressService {

    private final PolicyDocumentRepository policyDocumentRepository;
    private final PolicyDocumentProgressRepository progressRepository;

    public Set<String> completedKeys(Long userId, Long policyId) {
        return progressRepository
                .findByUserIdAndPolicyIdAndCompletedTrue(userId, policyId)
                .stream()
                .map(PolicyDocumentProgress::getDocumentKey)
                .collect(Collectors.toSet());
    }

    @Transactional
    public PolicyDocumentProgressResponse update(
            Long userId,
            Long policyId,
            Long policyDocumentId,
            boolean completed
    ) {
        PolicyDocument document = policyDocumentRepository
                .findByPolicyDocumentIdAndPolicy_PolicyId(policyDocumentId, policyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "체크할 서류를 찾을 수 없습니다."
                ));

        PolicyDocumentProgress progress = progressRepository
                .findByUserIdAndPolicyIdAndDocumentKey(
                        userId,
                        policyId,
                        document.getChecklistKey()
                )
                .orElseGet(PolicyDocumentProgress::new);

        progress.setUserId(userId);
        progress.setPolicyId(policyId);
        progress.setDocumentKey(document.getChecklistKey());
        progress.setCompleted(completed);
        progressRepository.save(progress);

        return new PolicyDocumentProgressResponse(
                policyId,
                policyDocumentId,
                completed
        );
    }
}
