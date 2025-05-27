package com.devtools.model.jpa;

import static org.apache.commons.lang3.StringUtils.trim;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
public class JpaDiscriminator {

    public static final int DEFAULT_DISCRIMINATOR_LENGTH = 31;

    private String type;
    private String column;
    private String value;
    private int length = DEFAULT_DISCRIMINATOR_LENGTH;

    public void setType(final String type) {
        if (StringUtils.isNotBlank(type)) {
            this.type = trim(type);
        }
    }

    public void setColumn(final String column) {
        if (StringUtils.isNotBlank(column)) {
            this.column = trim(column);
        }
    }

    public void setValue(final String value) {
        if (StringUtils.isNotBlank(value)) {
            this.value = trim(value);
        }
    }

    public void setLength(final String length) {
        if (StringUtils.isNotBlank(length)) {
            this.length = Integer.parseInt(trim(length));
        }
    }

}
