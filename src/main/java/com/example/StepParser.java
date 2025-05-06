package com.example;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class StepParser {
    private final StepMarkerDetector stepMarkerDetector = new StepMarkerDetector();
    private final DeclarationDetector declarationDetector = new DeclarationDetector();
    private final StepMetadataExtractor metadataExtractor = new StepMetadataExtractor();

    /**
     * Parses a PlantUML file and extracts steps.
     */
    public List<Step> parseFile(File file) throws IOException {
        // First pass: check if the file contains any step markers
        boolean hasStepMarkers = checkForStepMarkers(file);

        // Second pass: process the file based on whether it has step markers
        if (hasStepMarkers) {
            return processFileWithStepMarkers(file);
        } else {
            return processFileWithoutStepMarkers(file);
        }
    }

    /**
     * Checks if a file contains any step markers.
     */
    private boolean checkForStepMarkers(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (stepMarkerDetector.isStepMarker(line)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Processes a file that has step markers.
     */
    private List<Step> processFileWithStepMarkers(File file) throws IOException {
        StepBuilder builder = new StepBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                processLine(line, builder);
            }
        }

        return builder.build();
    }

    /**
     * Processes a file without step markers (creates a single default step).
     */
    private List<Step> processFileWithoutStepMarkers(File file) throws IOException {
        Step defaultStep = createDefaultStep(file);
        return Collections.singletonList(defaultStep);
    }

    /**
     * Creates a default step from a file without step markers.
     */
    private Step createDefaultStep(File file) throws IOException {
        StepMetadata metadata = new StepMetadata("Default Step", false, new HashMap<>());
        Step step = new Step(metadata);

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (declarationDetector.isDeclaration(line)) {
                    step.addDeclaration(line);
                } else if (!line.trim().isEmpty()) {
                    step.addContent(line);
                }
            }
        }

        return step;
    }

    /**
     * Processes a single line from the file.
     */
    private void processLine(String line, StepBuilder builder) {
        if (stepMarkerDetector.isStepMarker(line)) {
            StepMetadata metadata = metadataExtractor.extractMetadata(line);
            builder.startNewStep(metadata);
        } else if (declarationDetector.isDeclaration(line)) {
            builder.addDeclaration(line);
        } else if (!line.trim().isEmpty()) {
            builder.addContent(line);
        }
    }
}
//</codeFragment>

/**
 * Detects step markers in PlantUML files.
 */
class StepMarkerDetector {
    private static final Pattern STEP_PATTERN = Pattern.compile("'\\s*@step\\s+(\\{.*\\})");

    public boolean isStepMarker(String line) {
        return STEP_PATTERN.matcher(line).find();
    }

    public Matcher getStepMatcher(String line) {
        return STEP_PATTERN.matcher(line);
    }
}

class JSONParser {
    private final Gson gson = new Gson();

    public Map<String, Object> parse(String jsonStr) {
        try {
            Map<String, Object> map = gson.fromJson(jsonStr, Map.class);
            return map != null ? map : new HashMap<>();
        } catch (JsonParseException e) {
            System.err.println("Error parsing step metadata: " + e.getMessage());
            return new HashMap<>();
        }
    }
}

/**
 * Extracts metadata from step markers.
 */
class StepMetadataExtractor {
    private static final Pattern STEP_PATTERN = Pattern.compile("'\\s*@step\\s+(\\{.*\\})");
    private final JSONParser jsonParser = new JSONParser();

    public StepMetadata extractMetadata(String line) {
        Matcher matcher = STEP_PATTERN.matcher(line);
        if (matcher.find()) {
            String jsonStr = matcher.group(1);
            Map<String, Object> metadata = parseJson(jsonStr);

            String name = metadata.containsKey("name") ? metadata.get("name").toString() : "Unnamed Step";
            boolean newPage = metadata.containsKey("newPage") && Boolean.parseBoolean(metadata.get("newPage").toString());

            return new StepMetadata(name, newPage, metadata);
        }

        throw new IllegalArgumentException("Line does not contain a step marker: " + line);
    }

    private Map<String, Object> parseJson(String jsonStr) {
        Map<String, Object> map = jsonParser.parse(jsonStr);
        return map != null ? map : new HashMap<>();
    }
}

/**
 * Detects declarations in PlantUML files.
 */
class DeclarationDetector {
    private static final Pattern PARTICIPANT_PATTERN = Pattern.compile("(?i)^\\s*participant\\s+.*");
    private static final Pattern ACTOR_PATTERN = Pattern.compile("(?i)^\\s*actor\\s+.*");
    private static final Pattern INCLUDE_PATTERN = Pattern.compile("(?i)^\\s*!include\\s+.*");
    private static final Pattern QUOTED_PARTICIPANT_PATTERN = Pattern.compile("(?i)^\\s*participant\\s+\".*\"\\s+as\\s+.*");
    private static final Pattern QUOTED_ACTOR_PATTERN = Pattern.compile("(?i)^\\s*actor\\s+\".*\"\\s+as\\s+.*");

    public boolean isDeclaration(String line) {
        return PARTICIPANT_PATTERN.matcher(line).matches() ||
                ACTOR_PATTERN.matcher(line).matches() ||
                INCLUDE_PATTERN.matcher(line).matches() ||
                QUOTED_PARTICIPANT_PATTERN.matcher(line).matches() ||
                QUOTED_ACTOR_PATTERN.matcher(line).matches();
    }
}
