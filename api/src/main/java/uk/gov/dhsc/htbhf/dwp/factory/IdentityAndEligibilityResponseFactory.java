package uk.gov.dhsc.htbhf.dwp.factory;

import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.dwp.entity.uc.UCAdult;
import uk.gov.dhsc.htbhf.dwp.entity.uc.UCChild;
import uk.gov.dhsc.htbhf.dwp.entity.uc.UCHousehold;
import uk.gov.dhsc.htbhf.dwp.model.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static uk.gov.dhsc.htbhf.dwp.factory.IdentityVerificationUtils.areEqualIgnoringWhitespace;
import static uk.gov.dhsc.htbhf.dwp.factory.IdentityVerificationUtils.determineVerificationOutcome;
import static uk.gov.dhsc.htbhf.dwp.factory.IdentityVerificationUtils.firstSixCharacterMatch;
import static uk.gov.dhsc.htbhf.dwp.factory.IdentityVerificationUtils.matchingAdult;

@Component
public class IdentityAndEligibilityResponseFactory {

    private static final String NO_HOUSEHOLD_IDENTIFIER_PROVIDED = "";

    /**
     * Full details of how the identity and eligibility response is built can be found in README.md.
     *
     * @param household household to check
     * @param request   request to check
     * @return the response based on what's the request and the household in the database.
     */
    public IdentityAndEligibilityResponse determineIdentityAndEligibilityResponse(UCHousehold household, DWPEligibilityRequest request) {

        IdentityAndEligibilityResponse.IdentityAndEligibilityResponseBuilder builder = setupDefaultBuilder();
        PersonDTO person = request.getPerson();
        IdentityOutcome identityStatus = determineIdentityStatus(household, person, builder);
        EligibilityOutcome eligibilityStatus = determineAndSetEligibilityStatus(identityStatus, household, builder);

        if (IdentityOutcome.NOT_MATCHED == identityStatus || EligibilityOutcome.NOT_CONFIRMED == eligibilityStatus) {
            return builder.build();
        }

        UCAdult matchingAdult = household.getAdults().stream().filter(adult -> matchingAdult(adult, person)).findFirst().get();

        setAddressLine1VerificationOutcome(matchingAdult, person, builder);
        setPostcodeVerificationOutcome(matchingAdult, person, builder);

        builder.qualifyingBenefits(QualifyingBenefits.UNIVERSAL_CREDIT);
        setEmailVerificationOutcome(matchingAdult, person, builder);
        setMobileVerificationOutcome(matchingAdult, person, builder);
        setDobOfChildrenUnder4(household, builder);
        setPregnantDependantDob(person, builder);
        return builder.build();
    }

    private void setPregnantDependantDob(PersonDTO person, IdentityAndEligibilityResponse.IdentityAndEligibilityResponseBuilder builder) {
        if (person.getPregnantDependentDob() == null) {
            builder.pregnantChildDOBMatch(VerificationOutcome.NOT_SUPPLIED);
        }
    }

    //Make sure we only include those children which are under 4
    private void setDobOfChildrenUnder4(UCHousehold household, IdentityAndEligibilityResponse.IdentityAndEligibilityResponseBuilder builder) {
        LocalDate fourYearsAgo = LocalDate.now().minusYears(4);
        List<LocalDate> childrenDobs = household.getChildren().stream()
                .filter(child -> child.getDateOfBirth().isAfter(fourYearsAgo))
                .map(UCChild::getDateOfBirth)
                .collect(Collectors.toList());
        builder.dobOfChildrenUnder4(childrenDobs);
    }

    private void setMobileVerificationOutcome(UCAdult matchingAdult, PersonDTO person,
                                              IdentityAndEligibilityResponse.IdentityAndEligibilityResponseBuilder builder) {
        VerificationOutcome mobileVerificationOutcome = determineVerificationOutcome(
                matchingAdult.getMobilePhoneNumber(),
                person.getMobilePhoneNumber(),
                IdentityVerificationUtils::areEqual);
        builder.mobilePhoneMatch(mobileVerificationOutcome);
    }

    private void setEmailVerificationOutcome(UCAdult matchingAdult, PersonDTO person,
                                             IdentityAndEligibilityResponse.IdentityAndEligibilityResponseBuilder builder) {
        VerificationOutcome emailVerificationOutcome = determineVerificationOutcome(
                matchingAdult.getEmailAddress(),
                person.getEmailAddress(),
                IdentityVerificationUtils::areEqual);
        builder.emailAddressMatch(emailVerificationOutcome);
    }

    private void setPostcodeVerificationOutcome(UCAdult matchingAdult, PersonDTO person,
                                                IdentityAndEligibilityResponse.IdentityAndEligibilityResponseBuilder builder) {
        VerificationOutcome postcodeVerificationOutcome = (areEqualIgnoringWhitespace(matchingAdult.getPostcode(), person.getPostcode()))
                ? VerificationOutcome.MATCHED : VerificationOutcome.NOT_MATCHED;
        builder.postcodeMatch(postcodeVerificationOutcome);
    }

    private void setAddressLine1VerificationOutcome(UCAdult adult, PersonDTO person,
                                                    IdentityAndEligibilityResponse.IdentityAndEligibilityResponseBuilder builder) {
        VerificationOutcome addressLine1VerificationOutcome = (firstSixCharacterMatch(adult.getAddressLine1(), person.getAddressLine1()))
                ? VerificationOutcome.MATCHED : VerificationOutcome.NOT_MATCHED;
        builder.addressLine1Match(addressLine1VerificationOutcome);
    }

    private EligibilityOutcome determineAndSetEligibilityStatus(IdentityOutcome identityStatus,
                                                                UCHousehold household,
                                                                IdentityAndEligibilityResponse.IdentityAndEligibilityResponseBuilder builder) {
        EligibilityOutcome outcome = determineEligibilityOutcome(identityStatus, household);
        builder.eligibilityStatus(outcome);
        return outcome;
    }

    private EligibilityOutcome determineEligibilityOutcome(IdentityOutcome identityStatus, UCHousehold household) {
        if (identityStatus == IdentityOutcome.NOT_MATCHED) {
            return EligibilityOutcome.NOT_SET;
        } else if (household.isEarningsThresholdExceeded()) {
            return EligibilityOutcome.NOT_CONFIRMED;
        }
        return EligibilityOutcome.CONFIRMED;
    }

    private IdentityOutcome determineIdentityStatus(UCHousehold household, PersonDTO person,
                                                    IdentityAndEligibilityResponse.IdentityAndEligibilityResponseBuilder builder) {
        IdentityOutcome identityStatus = household.getAdults().stream().anyMatch(adult -> matchingAdult(adult, person))
                ? IdentityOutcome.MATCHED : IdentityOutcome.NOT_MATCHED;
        builder.identityStatus(identityStatus);
        return identityStatus;
    }

    private IdentityAndEligibilityResponse.IdentityAndEligibilityResponseBuilder setupDefaultBuilder() {
        return IdentityAndEligibilityResponse.builder()
                .identityStatus(IdentityOutcome.NOT_MATCHED)
                .eligibilityStatus(EligibilityOutcome.NOT_SET)
                .addressLine1Match(VerificationOutcome.NOT_SET)
                .postcodeMatch(VerificationOutcome.NOT_SET)
                .mobilePhoneMatch(VerificationOutcome.NOT_SET)
                .emailAddressMatch(VerificationOutcome.NOT_SET)
                .qualifyingBenefits(QualifyingBenefits.NOT_SET)
                .dobOfChildrenUnder4(emptyList())
                .householdIdentifier(NO_HOUSEHOLD_IDENTIFIER_PROVIDED)
                .pregnantChildDOBMatch(VerificationOutcome.NOT_SET)
                .deathVerificationFlag(DeathVerificationFlag.N_A);
    }
}
