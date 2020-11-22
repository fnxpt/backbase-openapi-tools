package com.backbase.oss.codegen.lint;

import com.backbase.oss.boat.quay.BoatLinter;
import com.backbase.oss.boat.quay.model.BoatLintReport;
import com.backbase.oss.codegen.AbstractDocumentationGenerator;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.Generator;


@Slf4j
public class BoatLintGenerator extends AbstractDocumentationGenerator {

    public BoatLintGenerator(BoatLintConfig config) {
        super(config);
    }

    @Override
    public Generator opts(ClientOptInput opts) {
        return this;
    }

    public List<File> generate() {

        BoatLinter boatLinter = new BoatLinter();

        File input = new File(this.input);
        if (!input.exists()) {
            throw new IllegalArgumentException("Input " + input + " does not exist");
        }

        try {
            BoatLintReport results = boatLinter.lint(input);
            return generate(results);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read file: " + input, e);
        }
    }

    @SneakyThrows
    private List<File> generate(BoatLintReport boatLintReport) {
        log.info("Generating BOAT Lint Report for: {}", boatLintReport.getTitle());

        // After processing our model, convert it into a map;
        Map<String, Object> bundle = convertToBundle(boatLintReport);
        List<File> files = processTemplates(bundle);
        log.info("Finished creating BOAT Yard for portal: {}", boatLintReport.getTitle());

        return files;
    }


}
