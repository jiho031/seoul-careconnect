package com.seoulcareconnect.integration.policy;

import com.seoulcareconnect.entity.policy.enums.AgeGroup;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExternalPolicyClassifierTest {

    private final ExternalPolicyClassifier classifier =
            new ExternalPolicyClassifier();

    @Test
    void allAgeRangesShouldReturnAll() {
        Set<AgeGroup> result = classifier.ageGroups(
                "만 20세 미만, "
                        + "만 20세 이상 ~ 만 39세 이하, "
                        + "만 40세 이상"
        );

        assertEquals(
                Set.of(AgeGroup.ALL),
                result
        );
    }

    @Test
    void ageFortyOrOlderShouldReturnMiddleAgeGroups() {
        Set<AgeGroup> result = classifier.ageGroups(
                "만 40세 이상"
        );

        assertEquals(
                Set.of(
                        AgeGroup.FORTIES,
                        AgeGroup.FIFTIES,
                        AgeGroup.SIXTIES_PLUS
                ),
                result
        );
    }

    @Test
    void ageTwentyToThirtyNineShouldReturnUnderForty() {
        Set<AgeGroup> result = classifier.ageGroups(
                "만 20세 이상 ~ 만 39세 이하"
        );

        assertEquals(
                Set.of(AgeGroup.UNDER_40),
                result
        );
    }
}