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
} 