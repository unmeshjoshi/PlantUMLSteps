package com.example;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for PlantUML files that extracts steps defined with step markers.
 * Step markers are comments in the format: ' @step {"name": "...", "newPage": true/false, ...}
 */
public class StepParser {
    // Pattern to match step markers: ' @step {"name": "...", ...}
    private static final Pattern STEP_PATTERN = Pattern.compile("'\\s*@step\\s+(\\{.*\\})");
    
    // Patterns to identify declaration lines (participants, actors, includes)
    private static final Pattern PARTICIPANT_PATTERN = Pattern.compile("(?i)^\\s*participant\\s+.*");
    private static final Pattern ACTOR_PATTERN = Pattern.compile("(?i)^\\s*actor\\s+.*");
    private static final Pattern INCLUDE_PATTERN = Pattern.compile("(?i)^\\s*!include\\s+.*");
    
    private final Gson gson = new Gson();
    
    /**
     * Parses a PlantUML file and extracts steps.
     * 
     * @param file The PlantUML file to parse
     * @return A list of extracted steps
     * @throws IOException If there's an error reading the file
     */
    public List<Step> parseFile(File file) throws IOException {
        List<Step> steps = new ArrayList<>();
        List<String> declarations = new ArrayList<>();
        List<String> contentBeforeFirstStep = new ArrayList<>();
        Step currentStep = null;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean hasStepMarkers = false;
            
            while ((line = reader.readLine()) != null) {
                Matcher stepMatcher = STEP_PATTERN.matcher(line);
                
                if (stepMatcher.find()) {
                    hasStepMarkers = true;
                    String jsonStr = stepMatcher.group(1);
                    Map<String, Object> metadata = parseStepMetadata(jsonStr);
                    
                    String name = metadata.containsKey("name") ? metadata.get("name").toString() : "Unnamed Step";
                    boolean newPage = metadata.containsKey("newPage") && Boolean.parseBoolean(metadata.get("newPage").toString());
                    
                    // Save the current step if it exists
                    if (currentStep != null) {
                        steps.add(currentStep);
                    }
                    
                    // Create a new step
                    currentStep = new Step(name, newPage, metadata);
                    
                    // Add all declarations to the new step
                    currentStep.addAllDeclarations(declarations);
                    
                    // If this isn't a new page and we have previous steps, copy all content from previous steps
                    if (!newPage && !steps.isEmpty()) {
                        for (Step previousStep : steps) {
                            for (String contentLine : previousStep.getContent()) {
                                currentStep.addContent(contentLine);
                            }
                        }
                    }
                    
                    // We don't add the step marker line to the content
                } else if (isDeclaration(line)) {
                    // Track declarations separately
                    declarations.add(line);
                    
                    if (currentStep != null) {
                        currentStep.addDeclaration(line);
                    } else {
                        // If we haven't encountered a step marker yet, save content for later
                        contentBeforeFirstStep.add(line);
                    }
                } else if (currentStep != null) {
                    // Add the line to the current step's content
                    currentStep.addContent(line);
                } else if (!line.trim().isEmpty()) {
                    // Store content before first step marker for later use
                    contentBeforeFirstStep.add(line);
                }
            }
            
            // Add the last step if it exists and hasn't been added yet
            if (currentStep != null) {
                steps.add(currentStep);
            }
            
            // If no step markers were found, create a default step with all content
            if (!hasStepMarkers) {
                Step defaultStep = new Step("Default Step", false, new HashMap<>());
                defaultStep.addAllDeclarations(declarations);
                
                for (String contentLine : contentBeforeFirstStep) {
                    if (isDeclaration(contentLine)) {
                        defaultStep.addDeclaration(contentLine);
                    } else {
                        defaultStep.addContent(contentLine);
                    }
                }
                
                return List.of(defaultStep);
            }
            
            return steps;
        }
    }
    
    /**
     * Parses the JSON metadata from a step marker.
     * 
     * @param jsonStr The JSON string to parse
     * @return A map containing the metadata
     */
    private Map<String, Object> parseStepMetadata(String jsonStr) {
        try {
            Map<String, Object> map = gson.fromJson(jsonStr, Map.class);
            return map != null ? map : new HashMap<>();
        } catch (JsonParseException e) {
            System.err.println("Error parsing step metadata: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Checks if a line is a declaration (participant, actor, or include).
     * 
     * @param line The line to check
     * @return true if the line is a declaration, false otherwise
     */
    private boolean isDeclaration(String line) {
        return PARTICIPANT_PATTERN.matcher(line).matches() ||
               ACTOR_PATTERN.matcher(line).matches() ||
               INCLUDE_PATTERN.matcher(line).matches();
    }
} 