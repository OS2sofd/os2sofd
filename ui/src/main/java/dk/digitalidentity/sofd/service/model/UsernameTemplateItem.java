package dk.digitalidentity.sofd.service.model;

import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.model.enums.UsernameTemplateVariableType;
import dk.digitalidentity.sofd.service.transliteration.Transliteration;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.math.NumberUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.stream.Stream;

@Getter
@Setter
public class UsernameTemplateItem {
    private UsernameTemplateVariableType usernameTemplateVariableType;
    private boolean uppercase;
    private String separator = ".";
    private String parameter;

    public String getValue(Person person, Affiliation affiliation, AtomicInteger remainingPermutations) {

        String result = switch(usernameTemplateVariableType) {
            case STATIC -> getStaticValue();
            case FIRSTNAME -> getLengthLimitedValue(person.getFirstname().replaceAll("\\s",""));
            case SURNAME -> getLengthLimitedValue(person.getSurname().replaceAll("\\s",""));
            case FULLNAME -> getLengthLimitedValue(person.getFirstname() + " " + person.getSurname());
            case CHOSENNAME -> getLengthLimitedValue(PersonService.getName(person));
            case NAMESEQUENCE -> getNameSequence(PersonService.getName(person), remainingPermutations);
            case LETTERS -> getLengthLimitedPermutation("abcdefghijklmnopqrstuvwxyz", remainingPermutations);
            case RANDOMLETTERS -> getRandomPermutation("abcdefghijklmnopqrstuvwxyz", remainingPermutations);
            case NUMBERS -> getLengthLimitedPermutation("0123456789", remainingPermutations);
            case SERIAL -> getSerial(remainingPermutations);
            case DATE -> getDateValue();
            case EMPLOYEEID -> affiliation != null && NumberUtils.isDigits(affiliation.getEmployeeId()) ? affiliation.getEmployeeId() : "";
        };

        return isUppercase() ? result.toUpperCase() : result.toLowerCase();
    }

    private String getNameSequence(String fullName, AtomicInteger remainingPermutations) {
        try {
            // Parse multiple length limits from parameter (e.g., "4,5,6")
            var lengthLimits = new ArrayList<Integer>();
            for (var limitParameter : parameter.split(",")) {
                var limit = Integer.parseInt(limitParameter.replaceAll("\\D", ""));
                if (limit >= 0) {
                    lengthLimits.add(limit);
                }
            }

            if (lengthLimits.isEmpty()) {
                return "";
            }

            var nameParts = Transliteration.transliterate(fullName, null).toLowerCase().split("\\s");
            var permutations = new LinkedHashSet<String>();

            // Generate permutations for each length limit
            for (int lengthLimit : lengthLimits) {
                generatePermutationsForLength(nameParts, lengthLimit, permutations);
            }

            var index = Math.min(remainingPermutations.get(), permutations.size() - 1);
            remainingPermutations.addAndGet(-index);
            return new ArrayList<>(permutations).get(index);
        } catch (Exception e) {
            return "";
        }
    }

    private void generatePermutationsForLength(String[] nameParts, int lengthLimit, Set<String> permutations) {
        // take 1,2 or 3 from firstname
        var prefixes = new LinkedHashSet<String>();
        var maxPrefixLength = Stream.of(3, nameParts[0].length(), lengthLimit).min(Integer::compare).get();
        for (int i = 0; i < maxPrefixLength; i++) {
            prefixes.add(nameParts[0].substring(0, i + 1));
        }

        var infixes = new LinkedHashSet<String>();
        infixes.add(""); // empty infix
        var previousInfix = "";
        for (int i = 1; i < nameParts.length - 1; i++) { // iterate the middle name parts
            var infix = previousInfix + nameParts[i].substring(0, 1); // only use first letter from each middle name
            infixes.add(infix);
            previousInfix = infix;
        }

        for (var prefix : prefixes) {
            for (var infix : infixes) {
                var infixPrefix = prefix + infix;
                if (nameParts.length > 1) { // we have a surname
                    // add the normal variant
                    var suffix = nameParts[nameParts.length - 1];
                    var permutation = infixPrefix + suffix;
                    permutation = permutation.substring(0, Math.min(permutation.length(), lengthLimit));
                    permutations.add(permutation);

                    // add a variant where we remove the first vowel from suffix
                    suffix = suffix.replaceFirst("(a|e|i|o|u)", "");
                    permutation = infixPrefix + suffix;
                    permutation = permutation.substring(0, Math.min(permutation.length(), lengthLimit));
                    permutations.add(permutation);
                } else {
                    var permutation = infixPrefix.substring(0, Math.min(infixPrefix.length(), lengthLimit));
                    permutations.add(permutation); // handles the case where we have only one name part for some reason
                }
            }
        }
    }


    private String getStaticValue() {
        var result = parameter == null ? "" : parameter;
        return isUppercase() ? result.toUpperCase() : result.toLowerCase();
    }

    private String getLengthLimitedValue(String value) {
        try {
            value = Transliteration.transliterate(value.trim(),null).replaceAll("\\s",separator);
            var lengthLimit = Integer.parseInt(parameter.replaceAll("\\D",""));
            return value.length() <= lengthLimit ? value : value.substring(0, lengthLimit);
        }catch(Exception e) {
            return value;
        }
    }

    private String getLengthLimitedPermutation(String charPool, AtomicInteger remainingPermutations) {
        try {
            int lengthLimit = Integer.parseInt(parameter.replaceAll("\\D", ""));
            int base = charPool.length();
            if (base < 1 || lengthLimit < 1) {
                return "";
            }

            int maxPermutations = (int) Math.pow(base, lengthLimit);
            int index = Math.min(remainingPermutations.get(), maxPermutations - 1); // clamp index

            char[] result = new char[lengthLimit];
            int tempIndex = index;
            for (int i = lengthLimit - 1; i >= 0; i--) {
                result[i] = charPool.charAt(tempIndex % base);
                tempIndex /= base;
            }

            remainingPermutations.addAndGet(-index);
            return new String(result);
        } catch (Exception e) {
            return "";
        }
    }

    private static AtomicInteger randomPermutationCounter = new AtomicInteger(1);

    private String getRandomPermutation(String charPool, AtomicInteger remainingPermutations) {
        try {
            int lengthLimit = Integer.parseInt(parameter.replaceAll("\\D", ""));
            int base = charPool.length();
            if (base < 1 || lengthLimit < 1) {
                return "";
            }

            int maxPermutations = (int) Math.pow(base, lengthLimit);
            int currentCount = remainingPermutations.get();
            if (currentCount <= 0) {
                return "";
            }

            // get sequential index and ensure it's positive
            int sequentialIndex = randomPermutationCounter.getAndIncrement();
            // handle overflow by using bitwise AND to keep it positive, then modulo
            sequentialIndex = (sequentialIndex & Integer.MAX_VALUE) % maxPermutations;

            int scrambledIndex = scrambleIndex(sequentialIndex, maxPermutations);

            // convert scrambled index to base-n representation
            char[] result = new char[lengthLimit];
            int tempIndex = scrambledIndex;
            for (int i = lengthLimit - 1; i >= 0; i--) {
                result[i] = charPool.charAt(tempIndex % base);
                tempIndex /= base;
            }

            remainingPermutations.decrementAndGet();
            return new String(result);
        } catch (Exception e) {
            return "";
        }
    }

    private int scrambleIndex(int index, int maxPermutations) {
        long prime = 2654435761L;
        return (int) ((index * prime) % maxPermutations);
    }

    private String getSerial(AtomicInteger remainingPermutations) {
        var result = remainingPermutations.get() == 0 ? "" : String.valueOf(remainingPermutations.get());
        remainingPermutations.set(0); // the serial exhausts all remaining permutations
        return result;
    }

    private String getDateValue() {
        var pattern = parameter == null ? "ddmm" : parameter;
        pattern = pattern.toLowerCase().replaceAll("[^dmy]",""); // remove anything that is not d,m or y
        pattern = pattern.replace("m","M"); // make sure we use month and not minute
        return LocalDate.now().format(DateTimeFormatter.ofPattern(pattern));
    }
}