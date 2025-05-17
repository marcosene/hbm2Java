package com.devtools.definition;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class JpaNamedQuery {

    private String name;
    private String query;
    private boolean nativeQuery = false;
    private final List<JpaColumn> returnColumns = new ArrayList<>();

    public void addReturnColumn(final JpaColumn returnColumn) {
        returnColumns.add(returnColumn);
    }
}
