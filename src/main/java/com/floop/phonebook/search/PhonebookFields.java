package com.floop.phonebook.search;

import java.util.Map;
import java.util.Set;

public final class PhonebookFields {

    public static final Map<String, String> ALLOWED = Map.ofEntries(
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

    public static final Set<String> TEXT = Set.of("name", "surname", "address");

    public static final Set<String> DATE = Set.of(
            "dateOfBirth", "activatedDate", "stopDate", "createdAt", "updatedAt");

    private PhonebookFields() { }
}
