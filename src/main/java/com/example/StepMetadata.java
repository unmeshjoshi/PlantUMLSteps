package com.example;

import java.util.Map;

/**
 * Container for step metadata attributes
 */
class StepMetadata {
    private final String name;
    private final boolean newPage;
    private final Map<String, Object> attributes;

    public StepMetadata(String name, boolean newPage, Map<String, Object> attributes) {
        this.name = name;
        this.newPage = newPage;
        this.attributes = attributes;
    }

    public String getName() {
        return name;
    }

    public boolean isNewPage() {
        return newPage;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
