package com.devtools.model.jpa;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
public class JpaDiscriminator {

    private String type;
    private String column;
    private String value;
    private int length = JpaDefaults.DEFAULT_DISCRIMINATOR_LENGTH;

    public void setType(final String type) {
        if (StringUtils.isNotBlank(type)) {
            this.type = type;
        }
    }

    public void setColumn(final String column) {
        if (StringUtils.isNotBlank(column)) {
            this.column = column;
        }
    }

    public void setValue(final String value) {
        if (StringUtils.isNotBlank(value)) {
            this.value = value;
        }
    }

    public void setLength(final String length) {
        if (StringUtils.isNotBlank(length)) {
            this.length = Integer.parseInt(length);
        }
    }

}
