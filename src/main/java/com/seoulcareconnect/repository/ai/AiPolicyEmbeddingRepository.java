package com.seoulcareconnect.repository.ai;

import com.seoulcareconnect.entity.ai.AiPolicyEmbedding;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AiPolicyEmbeddingRepository extends JpaRepository<AiPolicyEmbedding, Long> {

    @EntityGraph(attributePaths = "policy")
    List<AiPolicyEmbedding> findAllByPolicyPolicyIdIn(Collection<Long> policyIds);
}
