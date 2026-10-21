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


    private static final Set<SearchOperation> RELATIVE_TIME = EnumSet.of(
            SearchOperation.LAST_MINUTE, SearchOperation.LAST_HOUR, SearchOperation.LAST_DAY,
            SearchOperation.LAST_WEEK, SearchOperation.LAST_MONTH);

    private void validateDateTimeField(String field){

        if(!(field.equals("createdAt") || field.equals("updatedAt"))){
            throw new BadRequestException("LAST_HOUR and LAST_MINUTE are only supported for createdAt and updatedAt fields.This -> " + field +" field is ot supported");

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

    public Specification<PhonebookEntry> buildAll(List<SearchCriteria> criteriaList) {
        Specification<PhonebookEntry> result = Specification.unrestricted();
        if (criteriaList == null) {
            return result;
        }
        for (SearchCriteria criteria : criteriaList) {
            result = result.and(build(criteria));
        }
        return result;
    }

    public Specification<PhonebookEntry> buildAll(SearchRequest searchRequest) {
        return buildAll(searchRequest == null ? null : searchRequest.getCriteria());
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

        String field = PhonebookFields.ALLOWED.get(criteria.getKey().toLowerCase(Locale.ROOT));
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
                   return cb.greaterThan(root.get(field),comparable(criteria.getValue(),type));

                case LESS_THAN:
                    return cb.lessThan(root.get(field),comparable(criteria.getValue(),type));

                case GREATER_THAN_EQUAL:

                    return cb.greaterThanOrEqualTo(root.get(field),comparable(criteria.getValue(),type));

                case LESS_THAN_EQUAL:
                   return cb.lessThanOrEqualTo(root.get(field),comparable(criteria.getValue(),type));

                case BETWEEN:

                    if (!(criteria.getValue() instanceof List<?> values) || values.size()!=2) {
                        throw new BadRequestException("BETWEEN requires exactly 2 values");
                    }

                    Comparable startValue = comparable(values.get(0),type);
                    Comparable endValue = comparable(values.get(1),type);

                    return cb.between(root.get(field), startValue, endValue);


                case MATCH:

                   String value = likeText(criteria.getValue(),type);
                   String pattern = "%" + value + "%";
                   return cb.like(root.get(field), pattern);

                case MATCH_START:
                    String valueStart = likeText(criteria.getValue(),type);
                    String patternStart =  valueStart + "%";
                    return cb.like(root.get(field), patternStart);

                case MATCH_END:
                    String valueEnd = likeText(criteria.getValue(),type);
                    String patternEnd =  "%" + valueEnd ;
                    return cb.like(root.get(field), patternEnd);

                case NOT_MATCH:

                    String notMatch = likeText(criteria.getValue(),type);
                    String patternNotMatch = "%" + notMatch + "%";
                    return cb.notLike(root.get(field), patternNotMatch);

                case NOT_MATCH_START:
                    String notMatchStart = likeText(criteria.getValue(),type);
                    String patternNotMatchStart = notMatchStart + "%";
                    return cb.notLike(root.get(field), patternNotMatchStart);

                case NOT_MATCH_END:
                    String notMatchEnd = likeText(criteria.getValue(),type);
                    String patternNotMatchEnd = "%" + notMatchEnd;
                    return cb.notLike(root.get(field), patternNotMatchEnd);


                case LAST_MONTH:

                    LocalDate lastMonth = LocalDate.now().minusMonths(1);
                    return cb.greaterThanOrEqualTo(root.get(field),dateFrom(lastMonth,type));

                case LAST_WEEK:

                    LocalDate lastWeek = LocalDate.now().minusWeeks(1);
                    return cb.greaterThanOrEqualTo(root.get(field), dateFrom(lastWeek,type));

                case LAST_DAY:
                    LocalDate lastDay = LocalDate.now().minusDays(1);
                    return cb.greaterThanOrEqualTo(root.get(field), dateFrom(lastDay,type));

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

    @SuppressWarnings("rawtypes")
    private Comparable comparable(Object raw,Class<?> type){
        return (Comparable) convert(raw,type);
    }

    private String likeText(Object raw,Class<?> type){
        if (type!=String.class){
            throw new BadRequestException("MATCH operation are only supported for text fields");
        }
        if (!(raw instanceof String text)){
            throw new BadRequestException("MATCH operations require a text value");
        }
        return text;
    }

    @SuppressWarnings("rawtypes")
    private Comparable dateFrom(LocalDate date,Class<?> type){
        if (type == LocalDate.class) return date;

        if (type == LocalDateTime.class) {
            return date.atStartOfDay();
        }

        throw new BadRequestException("LAST_DAY, LAST_WEEK and LAST_MONTH are only supported for date fields");
    }

    private List<Object> convertAll(List<?> values, Class<?> type) {
        List<Object> result = new ArrayList<>();
        for (Object v : values) {
            result.add(convert(v,type));
        }
        return result;
    }
}
