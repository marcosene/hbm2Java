package com.devtools.definition;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class JpaPrimaryKey extends JpaAnnotation {

    private String type;
    private String name;
    private String columnName;
    private String generatorType;
    private String generatorName;
    private Integer initialValue;
    private Integer allocationSize = 1;

}
