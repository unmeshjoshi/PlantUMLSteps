package plantumlsteps.refactoring;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Parser for PlantUML files that extracts steps defined with step markers.
 * This version uses domain concepts and follows clean code practices.
 */
//<codeFragment name="step-parser">
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
        try {
            Map<String, Object> map = (Map<String, Object>) jsonParser.parse(jsonStr);
            return map != null ? map : new HashMap<>();
        } catch (ParseException e) {
            System.err.println("Error parsing step metadata: " + e.getMessage());
            return new HashMap<>();
        }
    }
}

/**
 * Detects declarations in PlantUML files.
 */
class DeclarationDetector {
    private static final Pattern PARTICIPANT_PATTERN = Pattern.compile("(?i)^\\s*participant\\s+.*");
    private static final Pattern ACTOR_PATTERN = Pattern.compile("(?i)^\\s*actor\\s+.*");
    private static final Pattern INCLUDE_PATTERN = Pattern.compile("(?i)^\\s*!include\\s+.*");
    
    public boolean isDeclaration(String line) {
        return PARTICIPANT_PATTERN.matcher(line).matches() ||
               ACTOR_PATTERN.matcher(line).matches() ||
               INCLUDE_PATTERN.matcher(line).matches();
    }
}

/**
 * Represents metadata for a step.
 */
class StepMetadata {
    private final String name;
    private final boolean newPage;
    private final Map<String, Object> attributes;
    
    public StepMetadata(String name, boolean newPage, Map<String, Object> attributes) {
        this.name = name;
        this.newPage = newPage;
        this.attributes = new HashMap<>(attributes);
    }
    
    public String getName() { return name; }
    public boolean isNewPage() { return newPage; }
    public Map<String, Object> getAttributes() { return Collections.unmodifiableMap(attributes); }
}

/**
 * Builder for creating steps.
 */
//<codeFragment name="step-builder">
class StepBuilder {
    private final List<Step> steps = new ArrayList<>();
    private final List<String> globalDeclarations = new ArrayList<>();
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
    
    public void addContent(String content) {
        // If no step has been started yet, create a default step
        if (currentStep == null) {
            StepMetadata metadata = new StepMetadata("Default Step", false, new HashMap<>());
            startNewStep(metadata);
        }
        
        currentStep.addContent(content);
    }
    
    public List<Step> build() {
        if (currentStep != null) {
            steps.add(currentStep);
        }
        
        return new ArrayList<>(steps);
    }
} 
//</codeFragment>