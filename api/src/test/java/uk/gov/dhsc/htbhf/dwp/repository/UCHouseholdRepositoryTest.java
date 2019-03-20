package uk.gov.dhsc.htbhf.dwp.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.dhsc.htbhf.dwp.entity.uc.UCHousehold;

import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.dwp.entity.UCHouseholdFactory.aHousehold;

@SpringBootTest
class UCHouseholdRepositoryTest {

    @Autowired
    UCHouseholdRepository repository;

    @PersistenceContext
    EntityManager em;

    @AfterEach
    void afterEach() {
        repository.deleteAll();
    }

    @Test
    void saveAndRetrieveHousehold() {
        //Given
        UCHousehold household = aHousehold();

        //When
        UCHousehold savedHousehold = repository.save(household);

        //Then
        assertThat(savedHousehold.getId()).isEqualTo(household.getId());
        household.getChildren().forEach(child -> assertThat(em.contains(child)));
        household.getAdults().forEach(adult -> assertThat(em.contains(adult)));
        assertThat(savedHousehold).isEqualTo(household);
        assertThat(savedHousehold).isEqualToComparingFieldByFieldRecursively(household);
    }

    @Test
    void shouldFindUCHouseholdByNino() {
        String nino = "QQ123456A";
        UCHousehold household = aHousehold();
        repository.save(household);

        Optional<UCHousehold> result = repository.findHouseholdByAdultWithNino(nino);

        assertThat(result.get()).isEqualTo(household);
    }

    @Test
    void shouldFailToFindUCHouseholdByNino() {
        Optional<UCHousehold> result = repository.findHouseholdByAdultWithNino("QQ123456C");

        assertThat(result.isEmpty()).isTrue();
    }

}
