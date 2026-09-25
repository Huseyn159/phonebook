package com.floop.phonebook.search;

import com.floop.phonebook.dto.SearchCriteria;
import com.floop.phonebook.dto.SearchRequest;
import com.floop.phonebook.entity.PhonebookEntry;
import com.floop.phonebook.exception.BadRequestException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;



@Component
public class PhonebookSpecificationBuilder {

    private static final Map<String, String> ALLOWED_FIELDS = Map.ofEntries(
            Map.entry("name", "name"),
            Map.entry("surname", "surname"),
            Map.entry("nationalid", "nationalId"),
            Map.entry("dateofbirth", "dateOfBirth"),
            Map.entry("fin", "fin"),
            Map.entry("address", "address"),
            Map.entry("city", "city"),
            Map.entry("number", "number"),
            Map.entry("activateddate", "activatedDate"),
            Map.entry("stopdate", "stopDate"),
            Map.entry("active", "active"),
            Map.entry("createdat", "createdAt"),
            Map.entry("updatedat", "updatedAt")
    );

    private void validateDateTimeField(String field){

        if(!(field.equals("createdAt") || field.equals("updatedAt"))){
            throw new BadRequestException("Field is invalid");

        }
    }

    public Specification<PhonebookEntry> buildAll(SearchRequest searchRequest) {
        Specification<PhonebookEntry> result = Specification.unrestricted();

        for (SearchCriteria criteria : searchRequest.getCriteria()) {
            Specification<PhonebookEntry> current = build(criteria);
            result = result.and(current);
        }

        return result;
    }

    public Specification<PhonebookEntry> build(SearchCriteria criteria) {
        String field = ALLOWED_FIELDS.get(criteria.getKey().toLowerCase());

        if (field == null) {
            throw new BadRequestException("Invalid search field: " + criteria.getKey());
        }

        return ((root, query, cb) -> {

            //--------------Case-lerde IS_MEMBER,ANY_OF heleki yoxdur,cunki phonebookda kollesiya tipli sahe yoxdur --------------

            switch (criteria.getOperation()) {

                case EQUAL:
                    return cb.equal(root.get(field), criteria.getValue());

                case NOT_EQUAL:
                    return cb.notEqual(root.get(field), criteria.getValue());

                case IN:
                    return root.get(field).in((List<?>) criteria.getValue());

                case GREATER_THAN:
                    Comparable minThreshold = (Comparable) criteria.getValue();
                    return cb.greaterThan(root.get(field),minThreshold);


                case LESS_THAN:

                    Comparable maxThreshold = (Comparable) criteria.getValue();
                    return cb.lessThan(root.get(field),maxThreshold);


                case GREATER_THAN_EQUAL:
                    Comparable minEqThreshold = (Comparable) criteria.getValue();
                    return cb.greaterThanOrEqualTo(root.get(field),minEqThreshold);

                case LESS_THAN_EQUAL:
                    Comparable maxEzThreshold = (Comparable) criteria.getValue();

                    return cb.lessThanOrEqualTo(root.get(field),maxEzThreshold);

                case NOT_IN:
                    Predicate inPredicate = root.get(field).in((List<?>) criteria.getValue());
                    return cb.not(inPredicate);

                case BETWEEN:
                    List<?> values = (List<?>) criteria.getValue();

                    if (values.size() != 2) {
                        throw new BadRequestException("Invalid number of values");
                    }
                    Comparable startValue = (Comparable) values.get(0);
                    Comparable endValue = (Comparable) values.get(1);

                    return cb.between(root.get(field), startValue, endValue);


                case MATCH:

                   String value = (String) criteria.getValue();
                   String pattern = "%" + value + "%";
                   return cb.like(root.get(field), pattern);

                case MATCH_START:
                    String valueStart = (String) criteria.getValue();
                    String patternStart =  valueStart + "%";
                    return cb.like(root.get(field), patternStart);

                case MATCH_END:
                    String valueEnd = (String) criteria.getValue();
                    String patternEnd =  "%" + valueEnd ;
                    return cb.like(root.get(field), patternEnd);

                case NOT_MATCH:

                    String notMatch = (String) criteria.getValue();
                    String patternNotMatch = "%" + notMatch + "%";
                    return cb.notLike(root.get(field), patternNotMatch);

                case NOT_MATCH_START:
                    String notMatchStart = (String) criteria.getValue();
                    String patternNotMatchStart = notMatchStart + "%";
                    return cb.notLike(root.get(field), patternNotMatchStart);

                case NOT_MATCH_END:
                    String notMatchEnd = (String) criteria.getValue();
                    String patternNotMatchEnd = "%" + notMatchEnd;
                    return cb.notLike(root.get(field), patternNotMatchEnd);


                case LAST_MONTH:

                    LocalDate lastMonth = LocalDate.now().minusMonths(1);
                    return cb.greaterThanOrEqualTo(root.get(field),lastMonth);

                case LAST_WEEK:

                    LocalDate lastWeek = LocalDate.now().minusWeeks(1);
                    return cb.greaterThanOrEqualTo(root.get(field), lastWeek);

                case LAST_DAY:
                    LocalDate lastDay = LocalDate.now().minusDays(1);
                    return cb.greaterThanOrEqualTo(root.get(field), lastDay);

                case LAST_HOUR:
                    validateDateTimeField(field);
                    LocalDateTime lastHour = LocalDateTime.now().minusHours(1);
                    return cb.greaterThanOrEqualTo(root.get(field), lastHour);

                case LAST_MINUTE:
                    validateDateTimeField(field);
                    LocalDateTime lastMinute = LocalDateTime.now().minusMinutes(1);
                    return cb.greaterThanOrEqualTo(root.get(field), lastMinute);



                default:
                    throw new BadRequestException("Unsupported operation: " + criteria.getOperation());


            }
        });


    }
}
