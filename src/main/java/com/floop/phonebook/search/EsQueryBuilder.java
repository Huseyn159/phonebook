package com.floop.phonebook.search;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.floop.phonebook.dto.SearchCriteria;
import com.floop.phonebook.exception.BadRequestException;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class EsQueryBuilder {

    private static final Set<String> TEXT_FIELDS = Set.of("name", "surname", "address");

    private String resolveField(SearchCriteria criteria){
        String key = criteria.getKey();
        if (key==null || key.isBlank()){
            throw new BadRequestException("Search key is required");
        }
        String field = PhonebookFields.ALLOWED.get(key.toLowerCase());
        if (field == null){
            throw new BadRequestException("Invalid search field: " + key);
        }
        return field;
    }
    private String exactField(String field){

        String fl = field.toLowerCase();
        if (TEXT_FIELDS.contains(fl)){
            return fl + ".raw";
        }
        else {
            return field;
        }

    }

    private boolean isTextField(String field) {
        return TEXT_FIELDS.contains(field.toLowerCase());
    }

    private List<?> toValueList(Object value){

        List<?> list;
        if (value instanceof List<?> l){
            list=l;
        }
        else{
            list = List.of(value);
        }
        if(list.isEmpty()){
            throw new BadRequestException("At least one vvalue is required");
        }
        return list;

    }

    private Query equalQuery(SearchCriteria criteria) {
        String field = resolveField(criteria);

        return Query.of(t->t
                .term(f->f
                        .field(exactField(field))
                        .value(String.valueOf(criteria.getValue()))));
    }

    private Query matchQuery(SearchCriteria criteria) {
        String field = resolveField(criteria);
        String value = String.valueOf(criteria.getValue());

//        if (isTextField(key)) {
//            return Query.of(q -> q.match(f -> f.field(key).query(value)));
//        }
//        return Query.of(q -> q.wildcard(w -> w.field(key).value("*" + value + "*")));

        if (!isTextField(field)){
            throw new BadRequestException("MATCH is only supported for text field:name,surname,address");
        }
        return Query.of(q->q.match(f->f.field(field).query(value)));
    }

    private Query inQuery(SearchCriteria criteria){
        String field = resolveField(criteria);
        List<FieldValue> fieldValues =toValueList(criteria.getValue()).stream().map(v-> FieldValue.of(String.valueOf(v))).toList();

        return Query.of(t->t
                .terms(f->f
                        .field(exactField(field))
                        .terms(v->v
                                .value(fieldValues))));


    }

}
