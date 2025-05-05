package plantumlsteps.refactoring;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the metadata for a step in a PlantUML sequence diagram.
 * Encapsulates properties like name, newPage flag, and custom attributes.
 */
public class StepMetadata {
    private final String name;
    private final boolean newPage;
    private final Map<String, Object> attributes;

    /**
     * Creates a new StepMetadata with the given name and newPage flag.
     */
    public StepMetadata(String name, boolean newPage) {
        this(name, newPage, new HashMap<>());
    }

    /**
     * Creates a new StepMetadata with the given name, newPage flag, and attributes.
     */
    public StepMetadata(String name, boolean newPage, Map<String, Object> attributes) {
        this.name = name;
        this.newPage = newPage;
        this.attributes = new HashMap<>(attributes);
    }

    /**
     * Returns the name of this step.
     */
    public String getName() {
        return name;
    }

    /**
     * Checks if this step should start on a new page.
     */
    public boolean isNewPage() {
        return newPage;
    }

    /**
     * Returns a read-only view of the attributes for this step.
     */
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    /**
     * Returns the value of the attribute with the given name, or null if it doesn't exist.
     */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * Returns the value of the attribute with the given name as a string, 
     * or null if it doesn't exist or isn't a string.
     */
    public String getAttributeAsString(String name) {
        Object value = attributes.get(name);
        return value instanceof String ? (String) value : null;
    }

    /**
     * Returns the value of the attribute with the given name as a boolean.
     * Returns false if the attribute doesn't exist or isn't a boolean.
     */
    public boolean getAttributeAsBoolean(String name) {
        Object value = attributes.get(name);
        return value instanceof Boolean ? (Boolean) value : false;
    }

    @Override
    public String toString() {
        return "StepMetadata{" +
               "name='" + name + '\'' +
               ", newPage=" + newPage +
               ", attributes=" + attributes +
               '}';
    }
} 