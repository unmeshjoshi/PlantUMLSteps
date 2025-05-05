package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a single step in a PlantUML sequence diagram.
 */
//<codeFragment name = "step-model">
public class Step {
    private String name;
    private boolean newPage;
    private Map<String, Object> metadata;
    private List<String> content;
    private List<String> declarations; // includes, actors, participants

    public Step(String name, boolean newPage, Map<String, Object> metadata) {
        this.name = name;
        this.newPage = newPage;
        this.metadata = metadata;
        this.content = new ArrayList<>();
        this.declarations = new ArrayList<>();
    }
//</codeFragment>
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public boolean isNewPage() {
        return newPage;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public List<String> getContent() {
        return content;
    }

    public void addContent(String line) {
        content.add(line);
    }

    public List<String> getDeclarations() {
        return declarations;
    }

    public void addDeclaration(String declaration) {
        declarations.add(declaration);
    }

    public void addAllDeclarations(List<String> declarations) {
        this.declarations.addAll(declarations);
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
                "name='" + name + '\'' +
                ", newPage=" + newPage +
                ", content size=" + content.size() +
                ", declarations size=" + declarations.size() +
                '}';
    }
} 