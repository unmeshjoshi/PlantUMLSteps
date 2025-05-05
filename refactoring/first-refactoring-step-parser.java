package plantumlsteps.refactoring;

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
        Step defaultStep = new Step("Default Step", false, new HashMap<>());
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
    
    /**
     * Container for step metadata attributes
     */
    private static class StepMetadata {
        private final String name;
        private final boolean newPage;
        private final Map<String, Object> attributes;
        
        public StepMetadata(String name, boolean newPage, Map<String, Object> attributes) {
            this.name = name;
            this.newPage = newPage;
            this.attributes = attributes;
        }
        
        public String getName() {
            return name;
        }
        
        public boolean isNewPage() {
            return newPage;
        }
        
        public Map<String, Object> getAttributes() {
            return attributes;
        }
    }
    //<codeFragment name="step-parser-refactored">

    /**
     * Represents a PlantUML document being parsed, managing state during parsing
     */
    private class PlantUmlDocument {
        private final List<Step> steps = new ArrayList<>();
        private final List<String> declarations = new ArrayList<>();
        private final List<String> contentBeforeFirstStep = new ArrayList<>();
        private Step currentStep = null;
        private boolean hasStepMarkers = false;
        
        public void beginNewStep(StepMetadata metadata) {
            hasStepMarkers = true;
            
            // Save the current step if it exists
            if (currentStep != null) {
                steps.add(currentStep);
            }
            
            // Create a new step
            currentStep = new Step(metadata.getName(), metadata.isNewPage(), metadata.getAttributes());
            
            // Add all declarations to the new step
            currentStep.addAllDeclarations(declarations);
            
            // If this isn't a new page and we have previous steps, copy all content from previous steps
            if (!metadata.isNewPage() && !steps.isEmpty()) {
                carryOverPreviousStepsContent();
            }
        }
        
        private void carryOverPreviousStepsContent() {
            for (Step previousStep : steps) {
                for (String contentLine : previousStep.getContent()) {
                    currentStep.addContent(contentLine);
                }
            }
        }
        
        public void addDeclaration(String line) {
            declarations.add(line);
            
            if (currentStep != null) {
                currentStep.addDeclaration(line);
            } else {
                contentBeforeFirstStep.add(line);
            }
        }
        
        public void addContentLine(String line) {
            if (currentStep != null) {
                currentStep.addContent(line);
            } else if (!line.trim().isEmpty()) {
                contentBeforeFirstStep.add(line);
            }
        }
        
        public void finalizeDocument() {
            // Add the last step if it exists and hasn't been added yet
            if (currentStep != null) {
                steps.add(currentStep);
            }
        }
        //</codeFragment>
        
        public boolean hasStepMarkers() {
            return hasStepMarkers;
        }
        
        public List<Step> getSteps() {
            return steps;
        }
        
        public List<String> getDeclarations() {
            return declarations;
        }
        
        public List<String> getContentBeforeFirstStep() {
            return contentBeforeFirstStep;
        }
    }
} 
