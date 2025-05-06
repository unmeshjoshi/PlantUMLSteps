package com.example;

import com.google.gson.Gson;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates step diagrams from PlantUML files with step markers.
 */
//<codeFragment name="step-diagram-generator">
public class StepDiagramGenerator {
    
    private static final String VIEWER_TEMPLATE = "/templates/viewer-template.html";
    
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: StepDiagramGenerator <input-puml-file> <output-directory>");
            System.exit(1);
        }
        
        String inputFile = args[0];
        String outputDir = args[1];
        
        try {
            generateStepDiagrams(new File(inputFile), new File(outputDir));
        } catch (IOException e) {
            System.err.println("Error generating step diagrams: " + e.getMessage());
            System.exit(1);
        }
    }
    
    /**
     * Generates step diagrams for a PlantUML file.
     * 
     * @param inputFile The input PlantUML file
     * @param outputDir The directory where step diagrams will be generated
     * @throws IOException If there's an error reading or writing files
     */
    public static void generateStepDiagrams(File inputFile, File outputDir) throws IOException {
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        // Read the style file from the root diagrams directory
        File styleFile = new File(inputFile.getParentFile().getParentFile(), "style.puml");
        String styleContent = "";
        if (styleFile.exists()) {
            styleContent = Files.readString(styleFile.toPath());
            // Remove @startuml and @enduml from style content if present
            styleContent = styleContent.replaceAll("@startuml\\s*", "").replaceAll("@enduml\\s*", "");
        }
        
        // Get the file name without extension
        String fileName = inputFile.getName();
        String dirName = fileName.substring(0, fileName.lastIndexOf('.'));
        
        // Create the target directory structure
        File targetDir = outputDir;
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        
        StepParser parser = new StepParser();
        List<Step> steps = parser.parseFile(inputFile);
        List<Map<String, String>> stepMetadata = new ArrayList<>();
        
        // Generate a diagram for each step
        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            String stepFileName = String.format("step-%02d-%s", 
                    i + 1, 
                    step.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-"));
            
            // Generate PlantUML file in the target directory
            File pumlFile = new File(targetDir, stepFileName + ".puml");
            try (FileWriter writer = new FileWriter(pumlFile)) {
                writer.write("@startuml\n");
                // Include the style content first
                if (!styleContent.isEmpty()) {
                    writer.write(styleContent);
                    writer.write("\n");
                }
                writer.write("title " + step.getName() + "\n\n");
                writer.write(step.generatePlantUML());
                writer.write("@enduml\n");
            }
            
            // Generate SVG file
            File svgFile = new File(targetDir, stepFileName + ".svg");
            generateSvg(pumlFile, svgFile);
            
            // Add step metadata for the viewer
            Map<String, String> metadata = new HashMap<>();
            metadata.put("name", step.getName());
            metadata.put("svgPath", stepFileName + ".svg");
            stepMetadata.add(metadata);
            
            System.out.println("Generated step diagram: " + pumlFile.getAbsolutePath());
            System.out.println("Generated SVG: " + svgFile.getAbsolutePath());
        }
        
        // Generate summary diagram in the target directory
        File summaryFile = new File(targetDir, "summary.puml");
        try (FileWriter writer = new FileWriter(summaryFile)) {
            writer.write("@startuml\n");
            writer.write("!theme plain\n");
            writer.write("title " + inputFile.getName() + " - Step Flow\n");
            writer.write("skinparam monochrome true\n");
            writer.write("skinparam shadowing false\n");
            writer.write("skinparam defaultFontName Arial\n");
            writer.write("skinparam defaultFontSize 12\n");
            writer.write("\n");
            
            for (int i = 0; i < steps.size(); i++) {
                Step step = steps.get(i);
                writer.write("rectangle \"" + step.getName() + "\" as step" + (i + 1) + "\n");
            }
            
            for (int i = 0; i < steps.size() - 1; i++) {
                writer.write("step" + (i + 1) + " --> step" + (i + 2) + "\n");
            }
            
            writer.write("@enduml\n");
        }
        
        // Generate SVG for summary
        File summarySvgFile = new File(targetDir, "summary.svg");
        generateSvg(summaryFile, summarySvgFile);
        
        // Generate HTML viewer in the target directory
        generateHtmlViewer(targetDir, stepMetadata);
        
        System.out.println("Generated step flow summary: " + summaryFile.getAbsolutePath());
        System.out.println("Generated HTML viewer: " + new File(targetDir, "index.html").getAbsolutePath());
    }
    
    /**
     * Generates an SVG file from a PlantUML file.
     * 
     * @param pumlFile The input PlantUML file
     * @param svgFile The output SVG file
     * @throws IOException If there's an error reading or writing files
     */
    private static void generateSvg(File pumlFile, File svgFile) throws IOException {
        String source = Files.readString(pumlFile.toPath());
        SourceStringReader reader = new SourceStringReader(source);
        try (FileOutputStream output = new FileOutputStream(svgFile)) {
            reader.outputImage(output, new FileFormatOption(FileFormat.SVG));
        }
    }
    
    /**
     * Generates an HTML viewer for the step diagrams.
     * 
     * @param outputDir The output directory
     * @param stepMetadata The metadata for each step
     * @throws IOException If there's an error reading or writing files
     */
    private static void generateHtmlViewer(File outputDir, List<Map<String, String>> stepMetadata) throws IOException {
        // Read the template
        InputStream templateStream = StepDiagramGenerator.class.getResourceAsStream(VIEWER_TEMPLATE);
        if (templateStream == null) {
            // Try loading from the filesystem as a fallback
            File templateFile = new File("src/main/resources" + VIEWER_TEMPLATE);
            if (templateFile.exists()) {
                templateStream = new FileInputStream(templateFile);
            } else {
                throw new IOException("Could not find viewer template at " + VIEWER_TEMPLATE + " or " + templateFile.getAbsolutePath());
            }
        }
        
        String template = new String(templateStream.readAllBytes());
        templateStream.close();
        
        // Replace the steps placeholder with the actual JSON data
        String stepsJson = new Gson().toJson(stepMetadata);
        String html = template.replace("{{STEPS_JSON}}", stepsJson);
        
        // Write the HTML file
        File htmlFile = new File(outputDir, "index.html");
        try (FileWriter writer = new FileWriter(htmlFile)) {
            writer.write(html);
        }
    }
} 
//</codeFragment>