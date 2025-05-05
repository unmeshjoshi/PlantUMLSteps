package plantumlsteps.refactoring;


//<codeFragment name="process-file-with-step-markers">
private List<Step> processFileWithStepMarkers(File file) throws IOException {
    ParsedPumlFile parsedFile = new ParsedPumlFile();
    
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
        String line;
        
        while ((line = reader.readLine()) != null) {
            if (stepMarkerDetector.isStepMarker(line)) {
                StepMetadata metadata = metadataExtractor.extractFrom(line);
                parsedFile.beginNewStep(metadata);
            } else if (declarationDetector.isDeclaration(line)) {
                parsedFile.addDeclaration(line);
            } else {
                parsedFile.addContentLine(line);
            }
        }
        
        parsedFile.finalizeProcessing();
        return parsedFile.getSteps();
    }
}

/**
 * Process a file that doesn't contain step markers
 */
private List<Step> processFileWithoutStepMarkers(File file) throws IOException {
    ParsedPumlFile parsedFile = new ParsedPumlFile();
    
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
        String line;
        
        while ((line = reader.readLine()) != null) {
            if (declarationDetector.isDeclaration(line)) {
                parsedFile.addDeclaration(line);
            } else if (!line.trim().isEmpty()) {
                parsedFile.addContentLine(line);
            }
        }
    }
    
    parsedFile.createDefaultStep();
    return parsedFile.getSteps();
}

/**
 * Represents a PlantUML file being parsed, encapsulating all parsing state
 */
class ParsedPumlFile {
    private final List<Step> steps = new ArrayList<>();
    private final List<String> declarations = new ArrayList<>();
    private final List<String> content = new ArrayList<>();
    private Step currentStep = null;
    
    /**
     * Begins a new step based on extracted metadata
     */
    public void beginNewStep(StepMetadata metadata) {
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
            copyPreviousStepsContent();
        }
    }
    
    /**
     * Copies content from previous steps to the current step
     */
    private void copyPreviousStepsContent() {
        for (Step previousStep : steps) {
            for (String contentLine : previousStep.getContent()) {
                currentStep.addContent(contentLine);
            }
        }
    }
    
    /**
     * Adds a declaration line to the parsed file
     */
    public void addDeclaration(String line) {
        declarations.add(line);
        
        if (currentStep != null) {
            currentStep.addDeclaration(line);
        }
    }
    
    /**
     * Adds a content line to the parsed file
     */
    public void addContentLine(String line) {
        if (currentStep != null) {
            currentStep.addContent(line);
        } else {
            content.add(line);
        }
    }
    
    /**
     * Finalizes processing by adding the last step if needed
     */
    public void finalizeProcessing() {
        if (currentStep != null) {
            steps.add(currentStep);
        }
    }
    
    /**
     * Creates a default step containing all declarations and content
     * Used when no step markers are found in the file
     */
    public void createDefaultStep() {
        Step defaultStep = new Step("Default Step", false, new HashMap<>());
        defaultStep.addAllDeclarations(declarations);
        
        for (String contentLine : content) {
            defaultStep.addContent(contentLine);
        }
        
        steps.add(defaultStep);
    }
    
    /**
     * Gets the list of steps extracted from the file
     */
    public List<Step> getSteps() {
        return steps;
    }
    
    /**
     * Gets the list of declarations found in the file
     */
    public List<String> getDeclarations() {
        return declarations;
    }
    
    /**
     * Gets the content collected when no steps were defined yet
     */
    public List<String> getContent() {
        return content;
    }
}
//</codeFragment>