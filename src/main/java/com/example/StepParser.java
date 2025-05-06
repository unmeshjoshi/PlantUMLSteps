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
//<codeFragment name="step-parser-refactored-step2">
class StepParser {
    private final StepMarkerDetector stepMarkerDetector = new StepMarkerDetector();
    private final DeclarationDetector declarationDetector = new DeclarationDetector();
    private final StepMetadataExtractor metadataExtractor = new StepMetadataExtractor();

    //</codeFragment>

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
            if (stepMarkerDetector.isStepMarker(line)) {
                StepMetadata metadata = metadataExtractor.extractFrom(line);
                document.beginNewStep(metadata);
            } else if (declarationDetector.isDeclaration(line)) {
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
            if (declarationDetector.isDeclaration(contentLine)) {
                defaultStep.addDeclaration(contentLine);
            } else {
                defaultStep.addContent(contentLine);
            }
        }

        return List.of(defaultStep);
    }
}

/**
 * Detects step markers in PlantUML content
 */
class StepMarkerDetector {
    private static final Pattern STEP_MARKER = Pattern.compile("'\\s*@step\\s+(\\{.*\\})");

    /**
     * Checks if a line contains a step marker
     */
    public boolean isStepMarker(String line) {
        return STEP_MARKER.matcher(line).find();
    }

    /**
     * Extracts the JSON string from a step marker line
     */
    public String extractJsonString(String line) {
        Matcher matcher = STEP_MARKER.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("Not a valid step marker: " + line);
    }
}

/**
 * Extracts and parses metadata from step markers
 */
class StepMetadataExtractor {
    private final StepMarkerDetector stepMarkerDetector = new StepMarkerDetector();
    private final JsonParser jsonParser = new JsonParser();

    /**
     * Extracts metadata from a step marker line
     */
    public StepMetadata extractFrom(String line) {
        String jsonStr = stepMarkerDetector.extractJsonString(line);
        Map<String, Object> attributes = jsonParser.parse(jsonStr);

        String name = getStringAttribute(attributes, "name", "Unnamed Step");
        boolean newPage = getBooleanAttribute(attributes, "newPage", false);

        return new StepMetadata(name, newPage, attributes);
    }

    private String getStringAttribute(Map<String, Object> attributes, String key, String defaultValue) {
        return attributes.containsKey(key) ? attributes.get(key).toString() : defaultValue;
    }

    private boolean getBooleanAttribute(Map<String, Object> attributes, String key, boolean defaultValue) {
        return attributes.containsKey(key) ? Boolean.parseBoolean(attributes.get(key).toString()) : defaultValue;
    }
}

/**
 * Parses JSON strings into maps
 */
class JsonParser {
    private final Gson gson = new Gson();

    /**
     * Parses a JSON string into a map
     */
    public Map<String, Object> parse(String jsonStr) {
        try {
            Map<String, Object> map = gson.fromJson(jsonStr, Map.class);
            return map != null ? map : new HashMap<>();
        } catch (JsonParseException e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
            return new HashMap<>();
        }
    }
}

/**
 * Detects declarations in PlantUML content
 */
class DeclarationDetector {
    private static final Pattern PARTICIPANT_DECLARATION = Pattern.compile("(?i)^\\s*participant\\s+.*");
    private static final Pattern ACTOR_DECLARATION = Pattern.compile("(?i)^\\s*actor\\s+.*");
    private static final Pattern INCLUDE_DECLARATION = Pattern.compile("(?i)^\\s*!include\\s+.*");

    /**
     * Checks if a line is a declaration (participant, actor, or include)
     */
    public boolean isDeclaration(String line) {
        return PARTICIPANT_DECLARATION.matcher(line).matches() ||
                ACTOR_DECLARATION.matcher(line).matches() ||
                INCLUDE_DECLARATION.matcher(line).matches();
    }

    /**
     * Gets the type of declaration
     */
    public DeclarationType getDeclarationType(String line) {
        if (PARTICIPANT_DECLARATION.matcher(line).matches()) {
            return DeclarationType.PARTICIPANT;
        } else if (ACTOR_DECLARATION.matcher(line).matches()) {
            return DeclarationType.ACTOR;
        } else if (INCLUDE_DECLARATION.matcher(line).matches()) {
            return DeclarationType.INCLUDE;
        }
        return DeclarationType.UNKNOWN;
    }

    public enum DeclarationType {
        PARTICIPANT, ACTOR, INCLUDE, UNKNOWN
    }
}
