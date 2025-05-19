package com.devtools.definition;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class JpaBase {

    private final List<JpaEntity> entities = new ArrayList<>();

    public void addEntity(final JpaEntity entity) {
        entities.add(entity);
    }
}
