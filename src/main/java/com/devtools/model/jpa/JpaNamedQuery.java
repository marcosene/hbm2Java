package com.devtools.model.jpa;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
public class JpaNamedQuery {

    private String name;
    private String query;
    private boolean nativeQuery = false;
    private final List<JpaColumn> returnColumns = new ArrayList<>();

    public void setName(final String name) {
        if (StringUtils.isNotBlank(name)) {
            this.name = name;
        }
    }

    public void setQuery(final String query) {
        if (StringUtils.isNotBlank(query)) {
            this.query = query;
        }
    }

    public void addReturnColumn(final JpaColumn returnColumn) {
        if (returnColumn != null && !returnColumns.contains(returnColumn)) {
            returnColumns.add(returnColumn);
        }
    }
}
