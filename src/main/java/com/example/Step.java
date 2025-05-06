package com.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a single step in a PlantUML sequence diagram.
 */
//<codeFragment name = "step-model">

/**
 * Represents a single step in a PlantUML sequence diagram.
 * This version works with StepMetadata for better encapsulation.
 */
public class Step {
    private final StepMetadata metadata;
    private final List<String> content;
    private final List<String> declarations;

    /**
     * Creates a new Step with the given metadata.
     */
    public Step(StepMetadata metadata) {
        this.metadata = metadata;
        this.content = new ArrayList<>();
        this.declarations = new ArrayList<>();
    }

    /**
     * Returns the name of this step.
     */
    public String getName() {
        return metadata.getName();
    }

    /**
     * Checks if this step should start on a new page.
     */
    public boolean isNewPage() {
        return metadata.isNewPage();
    }

    /**
     * Returns the metadata attributes for this step.
     */
    public Map<String, Object> getMetadata() {
        return metadata.getAttributes();
    }

    /**
     * Returns the content lines for this step.
     */
    public List<String> getContent() {
        return Collections.unmodifiableList(content);
    }

    /**
     * Adds a content line to this step.
     */
    public void addContent(String line) {
        // Skip @startuml and @enduml tags as they're added by the generator
        if (!isStartOrEndTag(line)) {
            content.add(line);
        }
    }

    /**
     * Returns the declarations for this step.
     */
    public List<String> getDeclarations() {
        return Collections.unmodifiableList(declarations);
    }

    /**
     * Adds a declaration to this step.
     */
    public void addDeclaration(String declaration) {
        // Skip @startuml and @enduml tags as they're added by the generator
        if (!isStartOrEndTag(declaration)) {
            declarations.add(declaration);
        }
    }

    /**
     * Adds all the given declarations to this step.
     */
    public void addAllDeclarations(List<String> declarations) {
        for (String declaration : declarations) {
            if (!isStartOrEndTag(declaration)) {
                this.declarations.add(declaration);
            }
        }
    }

    /**
     * Checks if a line is a @startuml or @enduml tag.
     */
    private boolean isStartOrEndTag(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("@startuml") || trimmed.equals("@enduml");
    }

    /**
     * Generates the complete PlantUML content for this step.
     */
    public String generatePlantUML() {
        StringBuilder builder = new StringBuilder();

        // Add declarations first
        for (String declaration : declarations) {
            builder.append(declaration).append("\n");
        }

        // Add content
        for (String line : content) {
            builder.append(line).append("\n");
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return "Step{" +
                "name='" + getName() + '\'' +
                ", newPage=" + isNewPage() +
                ", content size=" + content.size() +
                ", declarations size=" + declarations.size() +
                '}';
    }

    void addPreambleLines(List<String> preambleLines) {
        // Add preamble lines (like imports) to each step
        for (String preambleLine : preambleLines) {
            addContent(preambleLine);
        }
    }
}