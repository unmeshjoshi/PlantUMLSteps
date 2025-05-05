package com.example;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java -jar plantumlsequence.jar <puml-file>");
            System.out.println("Example PlantUML with steps: create a file with content:");
            System.out.println("@startuml");
            System.out.println("actor User");
            System.out.println("participant System");
            System.out.println("");
            System.out.println("' @step {\"name\": \"Step 1: User Login\", \"newPage\": false}");
            System.out.println("User -> System: Login Request");
            System.out.println("System --> User: Login Form");
            System.out.println("");
            System.out.println("' @step {\"name\": \"Step 2: Authentication\", \"newPage\": false}");
            System.out.println("User -> System: Submit Credentials");
            System.out.println("System --> User: Authentication Result");
            System.out.println("");
            System.out.println("' @step {\"name\": \"Step 3: Dashboard\", \"newPage\": true}");
            System.out.println("User -> System: View Dashboard");
            System.out.println("System --> User: Dashboard Data");
            System.out.println("@enduml");
            return;
        }

        File pumlFile = new File(args[0]);
        if (!pumlFile.exists()) {
            System.err.println("Error: File not found: " + pumlFile.getAbsolutePath());
            return;
        }

        try {
            StepParser parser = new StepParser();
            List<Step> steps = parser.parseFile(pumlFile);
            
            System.out.println("Found " + steps.size() + " steps in " + pumlFile.getName());
            
            for (int i = 0; i < steps.size(); i++) {
                Step step = steps.get(i);
                System.out.println("\nStep " + (i + 1) + ": " + step.getName());
                System.out.println("New Page: " + step.isNewPage());
                System.out.println("Declarations: " + step.getDeclarations().size());
                System.out.println("Content Lines: " + step.getContent().size());
                
                // Print the complete PlantUML for this step
                System.out.println("\nPlantUML Content:");
                System.out.println("@startuml");
                System.out.println(step.generatePlantUML());
                System.out.println("@enduml");
                System.out.println("--------------------");
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }
} 