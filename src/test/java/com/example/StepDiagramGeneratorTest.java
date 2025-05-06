package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class StepDiagramGeneratorTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    public void testGenerateStepDiagrams() throws IOException {
        // Create a test PlantUML file with step markers
        String pumlContent = "@startuml\n" +
                "actor User\n" +
                "participant System\n" +
                "participant Database\n\n" +
                "' @step {\"name\": \"Step 1: User Login\", \"newPage\": true}\n" +
                "User -> System: Login Request\n" +
                "System -> Database: Validate Credentials\n" +
                "Database --> System: Valid Credentials\n" +
                "System --> User: Login Success\n\n" +
                "' @step {\"name\": \"Step 2: Authentication\", \"newPage\": true}\n" +
                "User -> System: Request Resource\n" +
                "System -> System: Check Authentication\n" +
                "System --> User: Resource Access Granted\n\n" +
                "' @step {\"name\": \"Step 3: Dashboard\", \"newPage\": true}\n" +
                "User -> System: Load Dashboard\n" +
                "System -> Database: Fetch Dashboard Data\n" +
                "Database --> System: Return Dashboard Data\n" +
                "System --> User: Display Dashboard\n" +
                "@enduml";
        
        File tempFile = tempDir.resolve("test.puml").toFile();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(pumlContent);
        }
        
        // Create output directory
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        
        // Generate step diagrams
        StepDiagramGenerator.generateStepDiagrams(tempFile, outputDir.toFile());
        
        // Verify that step diagrams were generated
        List<Path> pumlFiles = Files.list(outputDir)
                .filter(path -> path.toString().endsWith(".puml"))
                .collect(Collectors.toList());
        
        List<Path> svgFiles = Files.list(outputDir)
                .filter(path -> path.toString().endsWith(".svg"))
                .collect(Collectors.toList());
        
        // We expect 4 PUML files (3 steps + summary) and 4 SVG files
        assertEquals(4, pumlFiles.size(), "Should have generated 4 PUML files");
        assertEquals(4, svgFiles.size(), "Should have generated 4 SVG files");
        
        // Check for specific files
        assertTrue(Files.exists(outputDir.resolve("step-01-step-1-user-login.puml")), "Step 1 PUML file should exist");
        assertTrue(Files.exists(outputDir.resolve("step-01-step-1-user-login.svg")), "Step 1 SVG file should exist");
        assertTrue(Files.exists(outputDir.resolve("step-02-step-2-authentication.puml")), "Step 2 PUML file should exist");
        assertTrue(Files.exists(outputDir.resolve("step-02-step-2-authentication.svg")), "Step 2 SVG file should exist");
        assertTrue(Files.exists(outputDir.resolve("step-03-step-3-dashboard.puml")), "Step 3 PUML file should exist");
        assertTrue(Files.exists(outputDir.resolve("step-03-step-3-dashboard.svg")), "Step 3 SVG file should exist");
        assertTrue(Files.exists(outputDir.resolve("summary.puml")), "Summary PUML file should exist");
        assertTrue(Files.exists(outputDir.resolve("summary.svg")), "Summary SVG file should exist");
        
        // Verify that HTML viewer was generated
        assertTrue(Files.exists(outputDir.resolve("index.html")), "HTML viewer should have been generated");
        
        // Verify that the HTML file references SVG files, not PNG files
        String htmlContent = Files.readString(outputDir.resolve("index.html"));
        assertTrue(htmlContent.contains(".svg"), "HTML should reference SVG files");
        assertFalse(htmlContent.contains(".png"), "HTML should not reference PNG files");
    }
    
    @Test
    public void testGeneratedFileStructure() throws IOException {
        // Create a test PlantUML file with step markers
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
        
        // Create output directory
        Path outputDir = tempDir.resolve("output_structure");
        Files.createDirectories(outputDir);
        
        // Generate step diagrams
        StepDiagramGenerator.generateStepDiagrams(tempFile, outputDir.toFile());
        
        // Verify that step diagrams were generated with correct structure
        List<Path> pumlFiles = Files.list(outputDir)
                .filter(path -> path.toString().endsWith(".puml"))
                .collect(Collectors.toList());
        
        // Verify structure of each generated step file
        for (Path pumlFile : pumlFiles) {
            if (!pumlFile.getFileName().toString().equals("summary.puml")) {
                verifyStepFileStructure(pumlFile);
            }
        }
        
        // Verify that SVG files were generated
        List<Path> svgFiles = Files.list(outputDir)
                .filter(path -> path.toString().endsWith(".svg"))
                .collect(Collectors.toList());
        
        assertEquals(4, svgFiles.size(), "Should have generated 4 SVG files");
        
        // Check for specific SVG files
        assertTrue(Files.exists(outputDir.resolve("step-01-step-1-initial-request.svg")), "Step 1 SVG file should exist");
        assertTrue(Files.exists(outputDir.resolve("step-02-step-2-process-request.svg")), "Step 2 SVG file should exist");
        assertTrue(Files.exists(outputDir.resolve("step-03-step-3-concurrent-access.svg")), "Step 3 SVG file should exist");
        assertTrue(Files.exists(outputDir.resolve("summary.svg")), "Summary SVG file should exist");
    }
    
    private void verifyStepFileStructure(Path pumlFile) throws IOException {
        String content = Files.readString(pumlFile);
        String[] lines = content.split("\n");
        
        // Count occurrences of @startuml and @enduml
        long startUmlCount = content.lines().filter(line -> line.trim().equals("@startuml")).count();
        long endUmlCount = content.lines().filter(line -> line.trim().equals("@enduml")).count();
        
        // Verify that there's exactly one @startuml at the beginning
        assertEquals(1, startUmlCount, "Should have exactly one @startuml");
        assertEquals("@startuml", lines[0].trim(), "@startuml should be the first line");
        
        // Verify that there's exactly one @enduml at the end
        assertEquals(1, endUmlCount, "Should have exactly one @enduml");
        assertEquals("@enduml", lines[lines.length - 1].trim(), "@enduml should be the last line");
        
        // Verify that all participants are included
        assertTrue(content.contains("actor \"User 1\" as U1"), "Should include User 1");
        assertTrue(content.contains("actor \"User 2\" as U2"), "Should include User 2");
        assertTrue(content.contains("participant \"System\" as S"), "Should include System");
        assertTrue(content.contains("participant \"Database\" as DB"), "Should include Database");
        
        // Verify that style include is present
        assertTrue(content.contains("!include @style.puml"), "Should include style.puml");
    }
}