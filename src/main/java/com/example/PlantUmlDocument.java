package com.example;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a PlantUML document being parsed, managing state during parsing
 */
class PlantUmlDocument {
    private final List<Step> steps = new ArrayList<>();
    private final List<String> declarations = new ArrayList<>();
    private final List<String> contentBeforeFirstStep = new ArrayList<>();
    private Step currentStep = null;
    private boolean hasStepMarkers = false;

    public void beginNewStep(StepMetadata metadata) {
        hasStepMarkers = true;

        // Save the current step if it exists
        if (currentStep != null) {
            steps.add(currentStep);
        }

        // Create a new step
        currentStep = new Step(metadata);

        // Add all declarations to the new step
        currentStep.addAllDeclarations(declarations);

        // If this isn't a new page and we have previous steps, copy all content from previous steps
        if (!metadata.isNewPage() && !steps.isEmpty()) {
            carryOverPreviousStepsContent();
        }
    }

    private void carryOverPreviousStepsContent() {
        for (Step previousStep : steps) {
            for (String contentLine : previousStep.getContent()) {
                currentStep.addContent(contentLine);
            }
        }
    }

    public void addDeclaration(String line) {
        declarations.add(line);

        if (currentStep != null) {
            currentStep.addDeclaration(line);
        } else {
            contentBeforeFirstStep.add(line);
        }
    }

    public void addContentLine(String line) {
        if (currentStep != null) {
            currentStep.addContent(line);
        } else if (!line.trim().isEmpty()) {
            contentBeforeFirstStep.add(line);
        }
    }

    public void finalizeDocument() {
        // Add the last step if it exists and hasn't been added yet
        if (currentStep != null) {
            steps.add(currentStep);
        }
    }
    //</codeFragment>

    public boolean hasStepMarkers() {
        return hasStepMarkers;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public List<String> getDeclarations() {
        return declarations;
    }

    public List<String> getContentBeforeFirstStep() {
        return contentBeforeFirstStep;
    }
}
