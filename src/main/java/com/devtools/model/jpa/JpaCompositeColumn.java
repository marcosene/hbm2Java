package com.devtools.model.jpa;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class JpaCompositeColumn extends JpaAbstract {

    private boolean optimisticLock = true;
    private final List<JpaColumn> columns = new ArrayList<>();

    public void addColumn(final JpaColumn column) {
        if (column != null && !columns.contains(column)) {
            columns.add(column);
        }
    }
}
