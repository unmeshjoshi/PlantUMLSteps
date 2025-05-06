package com.example;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for PlantUML files that extracts steps defined with step markers.
 * Step markers are comments in the format: ' @step {"name": "...", "newPage": true/false, ...}
 */
class StepParser {
    // Pattern to match step markers: ' @step {"name": "...", ...}
    private static final Pattern STEP_MARKER = Pattern.compile("'\\s*@step\\s+(\\{.*\\})");

    // Declaration patterns
    private static final Pattern PARTICIPANT_DECLARATION = Pattern.compile("(?i)^\\s*participant\\s+.*");
    private static final Pattern ACTOR_DECLARATION = Pattern.compile("(?i)^\\s*actor\\s+.*");
    private static final Pattern INCLUDE_DECLARATION = Pattern.compile("(?i)^\\s*!include\\s+.*");

    private final Gson gson = new Gson();

    /**
     * Parses a PlantUML file and extracts steps.
     *
     * @param file The PlantUML file to parse
     * @return A list of extracted steps
     * @throws IOException If there's an error reading the file
     */
    public List<Step> parseFile(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            PlantUmlDocument document = readPlantUmlDocument(reader);

            if (!document.hasStepMarkers()) {
                return createSingleDefaultStep(document);
            }

            return document.getSteps();
        }
    }

    /**
     * Reads and parses the entire PlantUML document
     */
    private PlantUmlDocument readPlantUmlDocument(BufferedReader reader) throws IOException {
        PlantUmlDocument document = new PlantUmlDocument();
        String line;

        while ((line = reader.readLine()) != null) {
            if (isStepMarker(line)) {
                StepMetadata metadata = extractStepMetadata(line);
                document.beginNewStep(metadata);
            } else if (isDeclaration(line)) {
                document.addDeclaration(line);
            } else {
                document.addContentLine(line);
            }
        }

        document.finalizeDocument();
        return document;
    }

    /**
     * Creates a default step containing all content when no step markers are found
     */
    private List<Step> createSingleDefaultStep(PlantUmlDocument document) {
        StepMetadata metadata = new StepMetadata("Default Step", false, new HashMap<>());
        Step defaultStep = new Step(metadata);
        defaultStep.addAllDeclarations(document.getDeclarations());

        for (String contentLine : document.getContentBeforeFirstStep()) {
            if (isDeclaration(contentLine)) {
                defaultStep.addDeclaration(contentLine);
            } else {
                defaultStep.addContent(contentLine);
            }
        }

        return List.of(defaultStep);
    }

    /**
     * Checks if a line contains a step marker
     */
    private boolean isStepMarker(String line) {
        return STEP_MARKER.matcher(line).find();
    }

    /**
     * Extracts metadata from a step marker line
     */
    private StepMetadata extractStepMetadata(String line) {
        Matcher matcher = STEP_MARKER.matcher(line);
        if (matcher.find()) {
            String jsonStr = matcher.group(1);
            Map<String, Object> attributes = parseStepAttributes(jsonStr);

            String name = extractAttributeAsString(attributes, "name", "Unnamed Step");
            boolean newPage = extractAttributeAsBoolean(attributes, "newPage", false);

            return new StepMetadata(name, newPage, attributes);
        }
        throw new IllegalArgumentException("Not a valid step marker: " + line);
    }

    /**
     * Parses the JSON attributes from a step marker.
     */
    private Map<String, Object> parseStepAttributes(String jsonStr) {
        try {
            Map<String, Object> map = gson.fromJson(jsonStr, Map.class);
            return map != null ? map : new HashMap<>();
        } catch (JsonParseException e) {
            System.err.println("Error parsing step metadata: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Extract a string attribute with default value
     */
    private String extractAttributeAsString(Map<String, Object> attributes, String key, String defaultValue) {
        return attributes.containsKey(key) ? attributes.get(key).toString() : defaultValue;
    }

    /**
     * Extract a boolean attribute with default value
     */
    private boolean extractAttributeAsBoolean(Map<String, Object> attributes, String key, boolean defaultValue) {
        return attributes.containsKey(key) ? Boolean.parseBoolean(attributes.get(key).toString()) : defaultValue;
    }

    /**
     * Checks if a line is a declaration (participant, actor, or include).
     */
    private boolean isDeclaration(String line) {
        return PARTICIPANT_DECLARATION.matcher(line).matches() ||
                ACTOR_DECLARATION.matcher(line).matches() ||
                INCLUDE_DECLARATION.matcher(line).matches();
    }

    //<codeFragment name="step-parser-refactored">

}