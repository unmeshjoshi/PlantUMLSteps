package com.example;

import java.util.ArrayList;
import java.util.List;

public class StepBuilder {
    private final List<Step> steps = new ArrayList<>();
    private final List<String> globalDeclarations = new ArrayList<>();
    private final List<String> header = new ArrayList<>();
    private final List<String> participantDeclarations = new ArrayList<>();
    private Step currentStep = null;

    public void startNewStep(StepMetadata metadata) {
        if (currentStep != null) {
            steps.add(currentStep);
        }

        currentStep = new Step(metadata);
        
        // Add global declarations to the step
        currentStep.addAllDeclarations(globalDeclarations);
        
        // Add header lines (like imports)
        currentStep.addPreambleLines(header);
        
        // Add participant declarations to ensure they're carried over between steps
        for (String participant : participantDeclarations) {
            currentStep.addContent(participant);
        }

        if (!metadata.isNewPage() && !steps.isEmpty()) {
            // Copy content from the previous step
            Step previousStep = steps.get(steps.size() - 1);
            for (String contentLine : previousStep.getContent()) {
                // Skip participant declarations that were already added
                if (!isParticipantDeclaration(contentLine)) {
                    currentStep.addContent(contentLine);
                }
            }
        }
    }

    private boolean isParticipantDeclaration(String line) {
        String trimmed = line.trim().toLowerCase();
        return trimmed.startsWith("participant ") || 
               trimmed.startsWith("actor ");
    }

    public void addDeclaration(String declaration) {
        globalDeclarations.add(declaration);

        // Also track participant declarations separately
        if (isParticipantDeclaration(declaration)) {
            participantDeclarations.add(declaration);
        }

        if (currentStep != null) {
            currentStep.addDeclaration(declaration);
        }
    }

    public void addContent(String line) {
        if (currentStep == null) {
            // Before the first step is defined
            addHeaderLine(line);
            
            // Also check if this is a participant declaration
            if (isParticipantDeclaration(line)) {
                participantDeclarations.add(line);
                globalDeclarations.add(line);
            }
            return;
        }
        
        currentStep.addContent(line);
    }

    private void addHeaderLine(String line) {
        // Before the first step, store lines like imports and includes
        // Skip @startuml and @enduml as they're added by the generator
        String trimmedLine = line.trim();
        if ((trimmedLine.startsWith("!import") || 
             trimmedLine.startsWith("!include")) && 
            !trimmedLine.startsWith("@startuml") && 
            !trimmedLine.startsWith("@enduml")) {
            header.add(line);
        }
    }

    public List<Step> build() {
        if (currentStep != null) {
            steps.add(currentStep);
        }

        return new ArrayList<>(steps);
    }
}