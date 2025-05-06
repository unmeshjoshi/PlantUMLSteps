package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StepDiagramGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    public void testGenerateStepDiagrams() throws IOException {
        // Create a test PlantUML file with step markers
        String pumlContent = "@startuml\n" +
                "' @step {\"name\": \"Step 1: User Login\", \"newPage\": false}\n" +
                "actor User\n" +
                "participant System\n\n" +
                "User -> System: Login Request\n" +
                "System --> User: Login Form\n\n" +
                "' @step {\"name\": \"Step 2: Authentication\", \"newPage\": false}\n" +
                "User -> System: Submit Credentials\n" +
                "System --> User: Authentication Result\n\n" +
                "' @step {\"name\": \"Step 3: Dashboard\", \"newPage\": true}\n" +
                "User -> System: View Dashboard\n" +
                "System --> User: Dashboard Data\n" +
                "@enduml";

        File inputFile = tempDir.resolve("test.puml").toFile();
        try (FileWriter writer = new FileWriter(inputFile)) {
            writer.write(pumlContent);
        }

        File outputDir = tempDir.resolve("output").toFile();
        
        // Generate step diagrams
        StepDiagramGenerator.generateStepDiagrams(inputFile, outputDir);
        
        // Verify that step files were created
        assertTrue(outputDir.exists(), "Output directory should exist");
        
        // Check for individual step files
        File step1File = new File(outputDir, "step-01-step-1-user-login.puml");
        File step2File = new File(outputDir, "step-02-step-2-authentication.puml");
        File step3File = new File(outputDir, "step-03-step-3-dashboard.puml");
        File summaryFile = new File(outputDir, "summary.puml");
        
        assertTrue(step1File.exists(), "Step 1 file should exist");
        assertTrue(step2File.exists(), "Step 2 file should exist");
        assertTrue(step3File.exists(), "Step 3 file should exist");
        assertTrue(summaryFile.exists(), "Summary file should exist");
        
        // Verify content of step 1 file
        String step1Content = Files.readString(step1File.toPath());
        assertTrue(step1Content.contains("@startuml"), "Step 1 should contain @startuml");
        assertTrue(step1Content.contains("title Step 1: User Login"), "Step 1 should have correct title");
        assertTrue(step1Content.contains("User -> System: Login Request"), "Step 1 should contain sequence");
        assertTrue(step1Content.contains("@enduml"), "Step 1 should contain @enduml");
        
        // Verify content of step 2 file
        String step2Content = Files.readString(step2File.toPath());
        assertTrue(step2Content.contains("@startuml"), "Step 2 should contain @startuml");
        assertTrue(step2Content.contains("title Step 2: Authentication"), "Step 2 should have correct title");
        assertTrue(step2Content.contains("User -> System: Submit Credentials"), "Step 2 should contain sequence");
        assertTrue(step2Content.contains("@enduml"), "Step 2 should contain @enduml");
        
        // Verify content of step 3 file
        String step3Content = Files.readString(step3File.toPath());
        assertTrue(step3Content.contains("@startuml"), "Step 3 should contain @startuml");
        assertTrue(step3Content.contains("title Step 3: Dashboard"), "Step 3 should have correct title");
        assertTrue(step3Content.contains("User -> System: View Dashboard"), "Step 3 should contain sequence");
        assertTrue(step3Content.contains("@enduml"), "Step 3 should contain @enduml");
        
        // Verify content of summary file
        String summaryContent = Files.readString(summaryFile.toPath());
        assertTrue(summaryContent.contains("@startuml"), "Summary should contain @startuml");
        assertTrue(summaryContent.contains("title test.puml - Step Flow"), "Summary should have correct title");
        assertTrue(summaryContent.contains("rectangle \"Step 1: User Login\""), "Summary should contain step 1");
        assertTrue(summaryContent.contains("rectangle \"Step 2: Authentication\""), "Summary should contain step 2");
        assertTrue(summaryContent.contains("rectangle \"Step 3: Dashboard\""), "Summary should contain step 3");
        assertTrue(summaryContent.contains("@enduml"), "Summary should contain @enduml");
    }
} 