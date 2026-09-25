package com.floop.phonebook.search;

public enum SearchOperation {
    EQUAL,
    NOT_EQUAL,
    GREATER_THAN,
    LESS_THAN,
    GREATER_THAN_EQUAL,
    LESS_THAN_EQUAL,
    MATCH,
    MATCH_START,
    MATCH_END,
    NOT_MATCH,
    NOT_MATCH_START,
    NOT_MATCH_END,
    IN,
    NOT_IN,
    BETWEEN,
    IS_MEMBER,
    ANY_OF,
    LAST_MINUTE,
    LAST_HOUR,
    LAST_DAY,
    LAST_WEEK,
    LAST_MONTH
}