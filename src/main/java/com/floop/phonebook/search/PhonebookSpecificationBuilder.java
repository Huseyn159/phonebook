package com.floop.phonebook.search;

import com.floop.phonebook.dto.SearchCriteria;
import com.floop.phonebook.dto.SearchRequest;
import com.floop.phonebook.entity.PhonebookEntry;
import com.floop.phonebook.exception.BadRequestException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;


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

    private static final Set<SearchOperation> RELATIVE_TIME = EnumSet.of(
            SearchOperation.LAST_MINUTE, SearchOperation.LAST_HOUR, SearchOperation.LAST_DAY,
            SearchOperation.LAST_WEEK, SearchOperation.LAST_MONTH);

    private void validateDateTimeField(String field){

        if(!(field.equals("createdAt") || field.equals("updatedAt"))){
            throw new BadRequestException("Field is invalid");

        }
    }

    private List<?> toList(Object value) {
        List<?> list;
        if (value instanceof List<?> l) {
            list = l;
        } else {
            list = List.of(value);
        }
        if (list.isEmpty()) {
            throw new BadRequestException("At least one value is required");
        }
        return list;
    }

    public Specification<PhonebookEntry> buildAll(SearchRequest searchRequest) {
        Specification<PhonebookEntry> result = Specification.unrestricted();

        if (searchRequest == null || searchRequest.getCriteria() == null) {
            return result;
        }
        for (SearchCriteria criteria : searchRequest.getCriteria()) {
            Specification<PhonebookEntry> current = build(criteria);
            result = result.and(current);
        }

        return result;
    }

    public Specification<PhonebookEntry> build(SearchCriteria criteria) {



        if (criteria == null) {
            throw new BadRequestException("Criteria item must not be null");
        }
        if (criteria.getKey() == null || criteria.getKey().isBlank()) {
            throw new BadRequestException("Search key is required");
        }
        if (criteria.getOperation() == null) {
            throw new BadRequestException("Search operation is required");
        }
        if (criteria.getValue() == null && !RELATIVE_TIME.contains(criteria.getOperation())) {
            throw new BadRequestException("Value is required for " + criteria.getOperation());
        }

        String field = ALLOWED_FIELDS.get(criteria.getKey().toLowerCase());
        if (field == null) {
            throw new BadRequestException("Invalid search field: " + criteria.getKey());
        }




        return ((root, query, cb) -> {
            //--------------Case-lerde IS_MEMBER,ANY_OF heleki yoxdur,cunki phonebookda kollesiya tipli sahe yoxdur --------------

            Class<?> type = root.get(field).getJavaType();
            switch (criteria.getOperation()) {

                case EQUAL:
                    return cb.equal(root.get(field), convert(criteria.getValue(), type));

                case NOT_EQUAL:
                    return cb.notEqual(root.get(field), convert(criteria.getValue(), type));

                case IN:
                    return root.get(field).in(convertAll(toList(criteria.getValue()), type));

                case NOT_IN:
                    Predicate inPredicate = root.get(field).in(convertAll(toList(criteria.getValue()), type));
                    return cb.not(inPredicate);

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


    private Object convert(Object raw,Class<?> type) {
        if (raw instanceof List<?>) {
            throw new BadRequestException("Only one value is required");
        }

        String text = String.valueOf(raw);

        if (type == LocalDate.class) {

            try {
                return LocalDate.parse(text);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("Invalid value for convert type to LocalDate: " + e.getMessage());
            }
        }
        if (type == LocalDateTime.class) {
            try {
                return LocalDateTime.parse(text);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("Invalid value for convert type to LocalDateTime: " + e.getMessage());
            }
        }
        if (type == Boolean.class ) {
            if (text.equals("true") || text.equals("false")){
            return Boolean.parseBoolean(text);}
            else {
                throw new BadRequestException("Expected true or false but got " + text);
            }
        }
        return text;
    }

    private List<Object> convertAll(List<?> values, Class<?> type) {
        List<Object> result = new ArrayList<>();
        for (Object v : values) {
            result.add(convert(v,type));
        }
        return result;
    }
}
