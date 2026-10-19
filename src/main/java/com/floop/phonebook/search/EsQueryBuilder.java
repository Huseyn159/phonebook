package com.floop.phonebook.search;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.floop.phonebook.dto.SearchCriteria;
import com.floop.phonebook.exception.BadRequestException;
import org.springframework.stereotype.Component;


import java.util.ArrayList;
import java.util.List;


import static com.floop.phonebook.search.PhonebookFields.TEXT;

@Component
public class EsQueryBuilder {




    public Query build(List<SearchCriteria> criterias){

        if (criterias == null || criterias.isEmpty()) return Query.of(q->q.matchAll(m->m));

        List<Query> filters = new ArrayList<>();

        for (SearchCriteria criteria : criterias){
            if (criteria == null) throw new BadRequestException("Criteria item must not be null");
            if (criteria.getOperation() == null) throw new BadRequestException("Search operation is required");
            if (criteria.getValue() == null) throw new BadRequestException("Value is required for " + criteria.getOperation() + " operation");


            switch (criteria.getOperation()){

                case EQUAL -> filters.add(equalQuery(criteria));
                case IN -> filters.add(inQuery(criteria));
                case MATCH -> filters.add(matchQuery(criteria));
                case GREATER_THAN,LESS_THAN_EQUAL,GREATER_THAN_EQUAL,LESS_THAN,BETWEEN -> filters.add(rangeQuery(criteria));
                default -> throw new BadRequestException("Unsupported operation type: " + criteria.getOperation());
            }
        }
        return Query.of(q->q.bool(b->b.filter(filters)));
    }







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
        if (TEXT.contains(fl)){
            return fl + ".raw";
        }
        else {
            return field;
        }

    }

    private boolean isTextField(String field) {
        return TEXT.contains(field.toLowerCase());
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
            throw new BadRequestException("At least one value is required");
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


    private Query rangeQuery(SearchCriteria criteria){
        String field = resolveField(criteria);
        if (!PhonebookFields.DATE.contains(field)){
            throw new BadRequestException("Range operators are only supported for date fields");
        }

        String value = String.valueOf(criteria.getValue());

        switch (criteria.getOperation()) {
            case GREATER_THAN -> {
                return    Query.of(q -> q.range(r -> r.date(d -> d.field(field).gt(value))));
            }
            case LESS_THAN -> {
                return Query.of(q->q.range(r->r.date(d->d.field(field).lt(value))));
            }
            case GREATER_THAN_EQUAL -> {
                return Query.of(q->q.range(r->r.date(d->d.field(field).gte(value))));
            }
            case LESS_THAN_EQUAL -> {
                return Query.of(q->q.range(r->r.date(d->d.field(field).lte(value))));
            }
            case BETWEEN -> {
                List<?> values = toValueList(criteria.getValue());
                if (values.size()!=2){
                     throw new BadRequestException("Please enter 2 value");
                }

            return Query.of(q->q.range(r->r.date(d->d.field(field)
                    .gte(String.valueOf(values.get(0)))
                    .lte(String.valueOf(values.get(1))))));
            }

            default -> throw new BadRequestException("Unsupported range operation: " + criteria.getOperation());

        }


    }

}
