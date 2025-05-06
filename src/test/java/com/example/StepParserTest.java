package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StepParserTest {

    @TempDir
    Path tempDir;

    @Test
    public void testParseFileWithSteps() throws IOException {
        // Create a test PlantUML file with step markers
        String pumlContent = "@startuml\n" +
                "actor User\n" +
                "participant System\n\n" +
                "' @step {\"name\": \"Step 1: User Login\", \"newPage\": false}\n" +
                "User -> System: Login Request\n" +
                "System --> User: Login Form\n\n" +
                "' @step {\"name\": \"Step 2: Authentication\", \"newPage\": false}\n" +
                "User -> System: Submit Credentials\n" +
                "System --> User: Authentication Result\n\n" +
                "' @step {\"name\": \"Step 3: Dashboard\", \"newPage\": true}\n" +
                "User -> System: View Dashboard\n" +
                "System --> User: Dashboard Data\n" +
                "@enduml";

        File tempFile = tempDir.resolve("test.puml").toFile();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(pumlContent);
        }

        // Parse the file
        StepParser parser = new StepParser();
        List<Step> steps = parser.parseFile(tempFile);

        // Verify results
        assertEquals(3, steps.size(), "Should have found 3 steps");

        // Check first step
        Step step1 = steps.get(0);
        assertEquals("Step 1: User Login", step1.getName());
        assertFalse(step1.isNewPage());
        assertEquals(2, step1.getDeclarations().size()); // actor and participant declarations
        assertTrue(step1.getContent().contains("User -> System: Login Request"));

        // Check second step
        Step step2 = steps.get(1);
        assertEquals("Step 2: Authentication", step2.getName());
        assertFalse(step2.isNewPage());
        assertEquals(2, step2.getDeclarations().size());
        assertTrue(step2.getContent().contains("User -> System: Submit Credentials"));
        // Second step should contain content from first step as well
        assertTrue(step2.getContent().contains("User -> System: Login Request"));

        // Check third step
        Step step3 = steps.get(2);
        assertEquals("Step 3: Dashboard", step3.getName());
        assertTrue(step3.isNewPage());
        assertEquals(2, step3.getDeclarations().size());
        assertTrue(step3.getContent().contains("User -> System: View Dashboard"));
        // Third step should NOT contain content from previous steps due to newPage=true
        assertFalse(step3.getContent().contains("User -> System: Login Request"));
    }

    @Test
    public void testParseFileWithoutSteps() throws IOException {
        // Create a test PlantUML file without step markers
        String pumlContent = "@startuml\n" +
                "actor User\n" +
                "participant System\n\n" +
                "User -> System: Login Request\n" +
                "System --> User: Login Form\n\n" +
                "User -> System: Submit Credentials\n" +
                "System --> User: Authentication Result\n\n" +
                "@enduml";

        File tempFile = tempDir.resolve("test_no_steps.puml").toFile();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(pumlContent);
        }

        // Parse the file
        StepParser parser = new StepParser();
        List<Step> steps = parser.parseFile(tempFile);

        // Verify results
        assertEquals(1, steps.size(), "Should have created one default step");
        
        Step defaultStep = steps.get(0);
        assertEquals("Default Step", defaultStep.getName());
        assertFalse(defaultStep.isNewPage());
        
        // Check that all content was captured
        assertTrue(defaultStep.getContent().contains("User -> System: Login Request"));
        assertTrue(defaultStep.getContent().contains("User -> System: Submit Credentials"));
    }

    @Test
    public void testGeneratedStepFilesStructure() throws IOException {
        // Create a test PlantUML file with step markers, participants, and includes
        String pumlContent = "@startuml\n" +
                "!include @style.puml\n" +
                "actor \"User 1\" as U1\n" +
                "actor \"User 2\" as U2\n" +
                "participant \"System\" as S\n" +
                "participant \"Database\" as DB\n\n" +
                "' @step {\"name\": \"Step 1: Initial Request\", \"newPage\": true}\n" +
                "U1 -> S: Initial Request\n" +
                "S --> U1: Request Received\n\n" +
                "' @step {\"name\": \"Step 2: Process Request\", \"newPage\": true}\n" +
                "S -> DB: Query Data\n" +
                "DB --> S: Return Results\n\n" +
                "' @step {\"name\": \"Step 3: Concurrent Access\", \"newPage\": true}\n" +
                "U2 -> S: Concurrent Request\n" +
                "S --> U2: Handle Concurrent Request\n" +
                "@enduml";

        File tempFile = tempDir.resolve("test_structure.puml").toFile();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(pumlContent);
        }

        // Parse the file
        StepParser parser = new StepParser();
        List<Step> steps = parser.parseFile(tempFile);

        // Verify steps were parsed correctly
        assertEquals(3, steps.size(), "Should have found 3 steps");

        // Generate PlantUML content for each step
        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            String generatedPuml = step.generatePlantUML();
            
            // Split into lines for easier testing
            String[] lines = generatedPuml.split("\n");
            
            // Verify structure
            assertTrue(lines.length > 2, "Step " + (i+1) + " should have more than 2 lines");
            
            // Count @startuml occurrences
            int startumlCount = 0;
            for (String line : lines) {
                if (line.trim().equals("@startuml")) {
                    startumlCount++;
                }
            }
            assertEquals(0, startumlCount, "Step " + (i+1) + " should not contain @startuml (it's added by the generator)");
            
            // Verify all participants are included
            assertTrue(generatedPuml.contains("actor \"User 1\" as U1"), "Step " + (i+1) + " should include User 1");
            assertTrue(generatedPuml.contains("actor \"User 2\" as U2"), "Step " + (i+1) + " should include User 2");
            assertTrue(generatedPuml.contains("participant \"System\" as S"), "Step " + (i+1) + " should include System");
            assertTrue(generatedPuml.contains("participant \"Database\" as DB"), "Step " + (i+1) + " should include Database");
            
            // Verify include directive is present
            assertTrue(generatedPuml.contains("!include @style.puml"), "Step " + (i+1) + " should include style.puml");
            
            // Verify step-specific content
            switch (i) {
                case 0:
                    assertTrue(generatedPuml.contains("U1 -> S: Initial Request"), "Step 1 should contain its specific content");
                    assertFalse(generatedPuml.contains("S -> DB: Query Data"), "Step 1 should not contain Step 2 content");
                    assertFalse(generatedPuml.contains("U2 -> S: Concurrent Request"), "Step 1 should not contain Step 3 content");
                    break;
                case 1:
                    assertFalse(generatedPuml.contains("U1 -> S: Initial Request"), "Step 2 should not contain Step 1 content (newPage=true)");
                    assertTrue(generatedPuml.contains("S -> DB: Query Data"), "Step 2 should contain its specific content");
                    assertFalse(generatedPuml.contains("U2 -> S: Concurrent Request"), "Step 2 should not contain Step 3 content");
                    break;
                case 2:
                    assertFalse(generatedPuml.contains("U1 -> S: Initial Request"), "Step 3 should not contain Step 1 content (newPage=true)");
                    assertFalse(generatedPuml.contains("S -> DB: Query Data"), "Step 3 should not contain Step 2 content (newPage=true)");
                    assertTrue(generatedPuml.contains("U2 -> S: Concurrent Request"), "Step 3 should contain its specific content");
                    break;
            }
        }
    }
}