package com.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class StepBuilder {
    private final List<Step> steps = new ArrayList<>();
    private final List<String> globalDeclarations = new ArrayList<>();
    private final List<String> content = new ArrayList<>();
    private Step currentStep = null;

    public void startNewStep(StepMetadata metadata) {
        if (currentStep != null) {
            steps.add(currentStep);
        }

        currentStep = new Step(metadata);
        currentStep.addAllDeclarations(globalDeclarations);

        if (!metadata.isNewPage() && !steps.isEmpty()) {
            // Copy content from the previous step
            Step previousStep = steps.get(steps.size() - 1);
            for (String contentLine : previousStep.getContent()) {
                currentStep.addContent(contentLine);
            }
        }
    }

    public void addDeclaration(String declaration) {
        globalDeclarations.add(declaration);

        if (currentStep != null) {
            currentStep.addDeclaration(declaration);
        }
    }

    public void addContent(String line) {
        if (currentStep != null) {
            currentStep.addContent(line);
        } else {
            content.add(line);
        }
    }

    public List<Step> build() {
        if (currentStep != null) {
            steps.add(currentStep);
        }

        return new ArrayList<>(steps);
    }
}