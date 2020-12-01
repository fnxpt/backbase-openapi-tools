/*
 * Copyright 2018 OpenAPI-Generator Contributors (https://openapi-generator.tech)
 * Copyright 2018 SmartBear Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.backbase.oss.codegen.angular;

import com.backbase.oss.codegen.doc.BoatCodegenParameter;
import com.backbase.oss.codegen.doc.BoatCodegenResponse;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.*;
import org.openapitools.codegen.languages.AbstractTypeScriptClientCodegen;
import org.openapitools.codegen.meta.FeatureSet;
import org.openapitools.codegen.meta.features.DocumentationFeature;
import org.openapitools.codegen.utils.ModelUtils;
import org.openapitools.codegen.utils.SemVer;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.openapitools.codegen.utils.StringUtils.*;

@Slf4j
public class BoatAngularGenerator extends AbstractTypeScriptClientCodegen {

    public static final String NPM_REPOSITORY = "npmRepository";
    public static final String WITH_INTERFACES = "withInterfaces";
    public static final String WITH_MOCKS = "withMocks";
    public static final String USE_SINGLE_REQUEST_PARAMETER = "useSingleRequestParameter";

    public static final String TAGGED_UNIONS = "taggedUnions";
    public static final String NG_VERSION = "ngVersion";
    public static final String FOUNDATION_VERSION = "foundationVersion";
    public static final String PROVIDED_IN_ROOT = "providedInRoot";
    public static final String ENFORCE_GENERIC_MODULE_WITH_PROVIDERS = "enforceGenericModuleWithProviders";
    public static final String API_MODULE_PREFIX = "apiModulePrefix";
    public static final String SERVICE_SUFFIX = "serviceSuffix";
    public static final String SERVICE_FILE_SUFFIX = "serviceFileSuffix";
    public static final String MODEL_SUFFIX = "modelSuffix";
    public static final String MODEL_FILE_SUFFIX = "modelFileSuffix";
    public static final String FILE_NAMING = "fileNaming";
    public static final String STRING_ENUMS = "stringEnums";
    public static final String STRING_ENUMS_DESC = "Generate string enums instead of objects for enum values.";
    public static final String BUILD_DIST = "buildDist";
    public static final String QUERY_PARAM_OBJECT_FORMAT = "queryParamObjectFormat";
    private static final String DEFAULT_IMPORT_PREFIX = "./";
    private static String CLASS_NAME_PREFIX_PATTERN = "^[a-zA-Z0-9]*$";
    private static String CLASS_NAME_SUFFIX_PATTERN = "^[a-zA-Z0-9]*$";
    private static String FILE_NAME_SUFFIX_PATTERN = "^[a-zA-Z0-9.-]*$";
    protected String foundationVersion = "6.0.0";
    protected String ngVersion = "10.0.0";
    protected String npmRepository = null;
    protected String serviceSuffix = "Service";
    protected String serviceFileSuffix = ".service";
    protected String modelSuffix = "";
    protected String modelFileSuffix = "";
    protected String fileNaming = "camelCase";
    protected Boolean stringEnums = false;
    protected QUERY_PARAM_OBJECT_FORMAT_TYPE queryParamObjectFormat = QUERY_PARAM_OBJECT_FORMAT_TYPE.dot;
    private boolean useSingleRequestParameter = true;
    private boolean taggedUnions = false;
    private FeatureSet featureSet;

    public BoatAngularGenerator() {
        super();

        modifyFeatureSet(features -> features.includeDocumentationFeatures(DocumentationFeature.Readme));

        this.outputFolder = "generated-code/boat-angular";

        supportsMultipleInheritance = true;

        embeddedTemplateDir = templateDir = "boat-angular";
        modelTemplateFiles.put("model.mustache", ".ts");
        apiTemplateFiles.put("api.service.mustache", ".ts");
        languageSpecificPrimitives.add("Blob");
        typeMapping.put("file", "Blob");
        apiPackage = "api";
        modelPackage = "model";

        this.cliOptions.add(new CliOption(NPM_REPOSITORY,
                "Use this property to set an url your private npmRepo in the package.json"));
        this.cliOptions.add(CliOption.newBoolean(WITH_INTERFACES,
                "Setting this property to true will generate interfaces next to the default class implementations.",
                false));
        this.cliOptions.add(CliOption.newBoolean(WITH_MOCKS,
                "Setting this property to true will generate mocks out of the examples.",
                false));
        this.cliOptions.add(CliOption.newBoolean(USE_SINGLE_REQUEST_PARAMETER,
                "Setting this property to true will generate functions with a single argument containing all API endpoint parameters instead of one argument per parameter.",
                false));
        this.cliOptions.add(CliOption.newBoolean(TAGGED_UNIONS,
                "Use discriminators to create tagged unions instead of extending interfaces.",
                this.taggedUnions));
        this.cliOptions.add(CliOption.newBoolean(PROVIDED_IN_ROOT,
                "Use this property to provide Injectables in root (it is only valid in angular version greater or equal to 6.0.0).",
                false));
        this.cliOptions.add(new CliOption(NG_VERSION, "The version of Angular. (At least 6.0.0)").defaultValue(this.ngVersion));
        this.cliOptions.add(new CliOption(FOUNDATION_VERSION, "The version of foundation-ang library.").defaultValue(this.foundationVersion));
        this.cliOptions.add(new CliOption(API_MODULE_PREFIX, "The prefix of the generated ApiModule."));
        this.cliOptions.add(new CliOption(SERVICE_SUFFIX, "The suffix of the generated service.").defaultValue(this.serviceSuffix));
        this.cliOptions.add(new CliOption(SERVICE_FILE_SUFFIX, "The suffix of the file of the generated service (service<suffix>.ts).").defaultValue(this.serviceFileSuffix));
        this.cliOptions.add(new CliOption(MODEL_SUFFIX, "The suffix of the generated model."));
        this.cliOptions.add(new CliOption(MODEL_FILE_SUFFIX, "The suffix of the file of the generated model (model<suffix>.ts)."));
        this.cliOptions.add(new CliOption(FILE_NAMING, "Naming convention for the output files: 'camelCase', 'kebab-case'.").defaultValue(this.fileNaming));
        this.cliOptions.add(new CliOption(STRING_ENUMS, STRING_ENUMS_DESC).defaultValue(String.valueOf(this.stringEnums)));
        this.cliOptions.add(new CliOption(BUILD_DIST, "Path to build package to"));
        this.cliOptions.add(new CliOption(QUERY_PARAM_OBJECT_FORMAT, "The format for query param objects: 'dot', 'json', 'key'.").defaultValue(this.queryParamObjectFormat.name()));
    }

    @Override
    protected void addAdditionPropertiesToCodeGenModel(CodegenModel codegenModel, Schema schema) {
        codegenModel.additionalPropertiesType = getTypeDeclaration(ModelUtils.getAdditionalProperties(schema));
        addImport(codegenModel, codegenModel.additionalPropertiesType);
    }

    @Override
    public String getName() {
        return "boat-angular";
    }

    @Override
    public String getHelp() {
        return "Generates a TypeScript Angular (6.x - 10.x) client library.";
    }

    @Override
    public void processOpts() {
        super.processOpts();
        supportingFiles.add(
                new SupportingFile("models.mustache", modelPackage().replace('.', File.separatorChar), "models.ts"));
        supportingFiles
                .add(new SupportingFile("apis.mustache", apiPackage().replace('.', File.separatorChar), "api.ts"));
        supportingFiles.add(new SupportingFile("public_api.mustache", getIndexDirectory(), "public_api.ts"));
        supportingFiles.add(new SupportingFile("api.module.mustache", getIndexDirectory(), "api.module.ts"));
        supportingFiles.add(new SupportingFile("configuration.mustache", getIndexDirectory(), "configuration.ts"));
        supportingFiles.add(new SupportingFile("variables.mustache", getIndexDirectory(), "variables.ts"));
        supportingFiles.add(new SupportingFile("encoder.mustache", getIndexDirectory(), "encoder.ts"));
        supportingFiles.add(new SupportingFile("gitignore", "", ".gitignore"));
        supportingFiles.add(new SupportingFile("git_push.sh.mustache", "", "git_push.sh"));
        supportingFiles.add(new SupportingFile("README.mustache", getIndexDirectory(), "README.md"));

        // determine NG version
        SemVer ngVersion;
        if (additionalProperties.containsKey(NG_VERSION)) {
            ngVersion = new SemVer(additionalProperties.get(NG_VERSION).toString());
        } else {
            ngVersion = new SemVer(this.ngVersion);
            log.info("generating code for Angular {} ...", ngVersion);
            log.info("  (you can select the angular version by setting the additionalProperty ngVersion)");
        }

        SemVer foundationVersion;
        if (additionalProperties.containsKey(FOUNDATION_VERSION)) {
            foundationVersion = new SemVer(additionalProperties.get(FOUNDATION_VERSION).toString());
        } else {
            foundationVersion = new SemVer(this.foundationVersion);
            log.info("generating code with foundation-ang {} ...", foundationVersion);
            log.info("  (you can select the angular version by setting the additionalProperty foundationVersion)");
        }

        if (additionalProperties.containsKey(NPM_NAME)) {
            addNpmPackageGeneration(ngVersion);
        }

        if (additionalProperties.containsKey(STRING_ENUMS)) {
            setStringEnums(Boolean.valueOf(additionalProperties.get(STRING_ENUMS).toString()));
            additionalProperties.put("stringEnums", getStringEnums());
            if (getStringEnums()) {
                classEnumSeparator = "";
            }
        }

        if (additionalProperties.containsKey(WITH_INTERFACES)) {
            boolean withInterfaces = Boolean.parseBoolean(additionalProperties.get(WITH_INTERFACES).toString());
            if (withInterfaces) {
                apiTemplateFiles.put("apiInterface.mustache", "Interface.ts");
            }
        }

        if (additionalProperties.containsKey(WITH_MOCKS)) {
            boolean withMocks = Boolean.parseBoolean(additionalProperties.get(WITH_MOCKS).toString());
            if (withMocks) {
                apiTemplateFiles.put("apiMocks.mustache", ".mocks.ts");
            }
        }

        if (additionalProperties.containsKey(USE_SINGLE_REQUEST_PARAMETER)) {
            this.setUseSingleRequestParameter(convertPropertyToBoolean(USE_SINGLE_REQUEST_PARAMETER));
        }
        writePropertyBack(USE_SINGLE_REQUEST_PARAMETER, getUseSingleRequestParameter());

        if (additionalProperties.containsKey(TAGGED_UNIONS)) {
            taggedUnions = Boolean.parseBoolean(additionalProperties.get(TAGGED_UNIONS).toString());
        }

        if (!additionalProperties.containsKey(PROVIDED_IN_ROOT)) {
            additionalProperties.put(PROVIDED_IN_ROOT, true);
        } else {
            additionalProperties.put(PROVIDED_IN_ROOT, Boolean.parseBoolean(
                    additionalProperties.get(PROVIDED_IN_ROOT).toString()
            ));
        }

        if (ngVersion.atLeast("9.0.0")) {
            additionalProperties.put(ENFORCE_GENERIC_MODULE_WITH_PROVIDERS, true);
        } else {
            additionalProperties.put(ENFORCE_GENERIC_MODULE_WITH_PROVIDERS, false);
        }

        additionalProperties.put(NG_VERSION, ngVersion);
        additionalProperties.put(FOUNDATION_VERSION, foundationVersion);

        if (additionalProperties.containsKey(API_MODULE_PREFIX)) {
            String apiModulePrefix = additionalProperties.get(API_MODULE_PREFIX).toString();
            validateClassPrefixArgument("ApiModule", apiModulePrefix);

            additionalProperties.put("apiModuleClassName", apiModulePrefix + "ApiModule");
            additionalProperties.put("configurationClassName", apiModulePrefix + "Configuration");
            additionalProperties.put("configurationParametersInterfaceName", apiModulePrefix + "ConfigurationParameters");
            additionalProperties.put("apiModulePrefix", true);
        } else {
            additionalProperties.put("apiModuleClassName", "ApiModule");
            additionalProperties.put("configurationClassName", "Configuration");
            additionalProperties.put("configurationParametersInterfaceName", "ConfigurationParameters");
            additionalProperties.put("apiModulePrefix", false);
        }
        if (additionalProperties.containsKey(SERVICE_SUFFIX)) {
            serviceSuffix = additionalProperties.get(SERVICE_SUFFIX).toString();
            validateClassSuffixArgument("Service", serviceSuffix);
        }
        if (additionalProperties.containsKey(SERVICE_FILE_SUFFIX)) {
            serviceFileSuffix = additionalProperties.get(SERVICE_FILE_SUFFIX).toString();
            validateFileSuffixArgument("Service", serviceFileSuffix);
        }
        if (additionalProperties.containsKey(MODEL_SUFFIX)) {
            modelSuffix = additionalProperties.get(MODEL_SUFFIX).toString();
            validateClassSuffixArgument("Model", modelSuffix);
        }
        if (additionalProperties.containsKey(MODEL_FILE_SUFFIX)) {
            modelFileSuffix = additionalProperties.get(MODEL_FILE_SUFFIX).toString();
            validateFileSuffixArgument("Model", modelFileSuffix);
        }
        if (additionalProperties.containsKey(FILE_NAMING)) {
            this.setFileNaming(additionalProperties.get(FILE_NAMING).toString());
        }
        if (!additionalProperties.containsKey(BUILD_DIST)) {
            additionalProperties.put(BUILD_DIST, "dist");
        }

        if (additionalProperties.containsKey(QUERY_PARAM_OBJECT_FORMAT)) {
            setQueryParamObjectFormat((String) additionalProperties.get(QUERY_PARAM_OBJECT_FORMAT));
        }
        additionalProperties.put("isQueryParamObjectFormatDot", getQueryParamObjectFormatDot());
        additionalProperties.put("isQueryParamObjectFormatJson", getQueryParamObjectFormatJson());
        additionalProperties.put("isQueryParamObjectFormatKey", getQueryParamObjectFormatKey());

    }

    private void addNpmPackageGeneration(SemVer ngVersion) {

        if (additionalProperties.containsKey(NPM_REPOSITORY)) {
            this.setNpmRepository(additionalProperties.get(NPM_REPOSITORY).toString());
        }

        // Set the typescript version compatible to the Angular version
        if (ngVersion.atLeast("10.0.0")) {
            additionalProperties.put("tsVersion", ">=3.9.2 <4.0.0");
        } else if (ngVersion.atLeast("9.0.0")) {
            additionalProperties.put("tsVersion", ">=3.6.0 <3.8.0");
        } else if (ngVersion.atLeast("8.0.0")) {
            additionalProperties.put("tsVersion", ">=3.4.0 <3.6.0");
        } else if (ngVersion.atLeast("7.0.0")) {
            additionalProperties.put("tsVersion", ">=3.1.1 <3.2.0");
        } else {
            // Angular v6 requires typescript ">=2.7.2 and <2.10.0"
            additionalProperties.put("tsVersion", ">=2.7.2 and <2.10.0");
        }

        // Set the rxJS version compatible to the Angular version
        if (ngVersion.atLeast("10.0.0")) {
            additionalProperties.put("rxjsVersion", "6.6.0");
        } else if (ngVersion.atLeast("9.0.0")) {
            additionalProperties.put("rxjsVersion", "6.5.3");
        } else if (ngVersion.atLeast("8.0.0")) {
            additionalProperties.put("rxjsVersion", "6.5.0");
        } else if (ngVersion.atLeast("7.0.0")) {
            additionalProperties.put("rxjsVersion", "6.3.0");
        } else {
            // Angular v6
            additionalProperties.put("rxjsVersion", "6.1.0");
        }

        // Specific ng-packagr configuration
        if (ngVersion.atLeast("10.0.0")) {
            additionalProperties.put("ngPackagrVersion", "10.0.3");
            additionalProperties.put("tsickleVersion", "0.39.1");
        } else if (ngVersion.atLeast("9.0.0")) {
            additionalProperties.put("ngPackagrVersion", "9.0.1");
            additionalProperties.put("tsickleVersion", "0.38.0");
        } else if (ngVersion.atLeast("8.0.0")) {
            additionalProperties.put("ngPackagrVersion", "5.4.0");
            additionalProperties.put("tsickleVersion", "0.35.0");
        } else if (ngVersion.atLeast("7.0.0")) {
            // compatible versions with typescript version
            additionalProperties.put("ngPackagrVersion", "5.1.0");
            additionalProperties.put("tsickleVersion", "0.34.0");
        } else {
            // angular v6
            // compatible versions with typescript version
            additionalProperties.put("ngPackagrVersion", "3.0.6");
            additionalProperties.put("tsickleVersion", "0.32.1");
        }

        // set zone.js version
        if (ngVersion.atLeast("9.0.0")) {
            additionalProperties.put("zonejsVersion", "0.10.2");
        } else if (ngVersion.atLeast("8.0.0")) {
            additionalProperties.put("zonejsVersion", "0.9.1");
        } else {
            // compatible versions to Angular 6+
            additionalProperties.put("zonejsVersion", "0.8.26");
        }

        //Files for building our lib
        supportingFiles.add(new SupportingFile("package.mustache", getIndexDirectory(), "package.json"));
        supportingFiles.add(new SupportingFile("tsconfig.mustache", getIndexDirectory(), "tsconfig.json"));
    }

    private String getIndexDirectory() {
        String indexPackage = modelPackage.substring(0, Math.max(0, modelPackage.lastIndexOf('.')));
        return indexPackage.replace('.', File.separatorChar);
    }

    public Boolean getStringEnums() {
        return stringEnums;
    }

    public void setStringEnums(boolean value) {
        stringEnums = value;
    }

    public boolean getQueryParamObjectFormatDot() {
        return QUERY_PARAM_OBJECT_FORMAT_TYPE.dot.equals(queryParamObjectFormat);
    }

    public boolean getQueryParamObjectFormatJson() {
        return QUERY_PARAM_OBJECT_FORMAT_TYPE.json.equals(queryParamObjectFormat);
    }

    public boolean getQueryParamObjectFormatKey() {
        return QUERY_PARAM_OBJECT_FORMAT_TYPE.key.equals(queryParamObjectFormat);
    }

    @Override
    public boolean isDataTypeFile(final String dataType) {
        return dataType != null && dataType.equals("Blob");
    }

    @Override
    public String getTypeDeclaration(Schema p) {
        if (ModelUtils.isFileSchema(p)) {
            return "Blob";
        } else {
            return super.getTypeDeclaration(p);
        }
    }

    private String applyLocalTypeMapping(String type) {
        if (typeMapping.containsKey(type)) {
            type = typeMapping.get(type);
        }
        return type;
    }

    @Override
    public void postProcessParameter(CodegenParameter parameter) {
        super.postProcessParameter(parameter);
        parameter.dataType = applyLocalTypeMapping(parameter.dataType);
    }

    @Override
    public Map<String, Object> postProcessOperationsWithModels(Map<String, Object> operations, List<Object> allModels) {
        Map<String, Object> objs = (Map<String, Object>) operations.get("operations");
        Map<String, Map<String, Object>> pathOperations = new HashMap<String, Map<String, Object>>();

        // Add filename information for api imports
        objs.put("apiFilename", getApiFilenameFromClassname(objs.get("classname").toString()));

        List<CodegenOperation> ops = (List<CodegenOperation>) objs.get("operation");
        boolean hasSomeFormParams = false;
        for (CodegenOperation op : ops) {
            if (op.getHasFormParams()) {
                hasSomeFormParams = true;
            }
            op.httpMethod = op.httpMethod.toLowerCase(Locale.ENGLISH);


            // Prep a string buffer where we're going to set up our new version of the string.
            StringBuilder pathBuffer = new StringBuilder();
            StringBuilder parameterName = new StringBuilder();
            int insideCurly = 0;

            Map<String, Object> pathOp = new HashMap<>();
            pathOp.put("pathName", removeNonNameElementToCamelCase(op.path.replaceAll("[\\{\\}\\/]", "-")));
            pathOp.put("pattern", op.path);
            pathOp.put("hasExamples", op.examples != null && !op.examples.isEmpty());
            pathOp.put("operations", Arrays.asList(op));
            pathOperations.merge(op.path, pathOp, (o1, o2) -> {
                o1.put("operations", Stream.of(
                        (List<CodegenOperation>) o1.get("operations"),
                        (List<CodegenOperation>) o2.get("operations")
                ).flatMap(oper -> oper.stream())
                        .collect(Collectors.toList()));
                o1.put("hasExamples",
                        (boolean) o1.get("hasExamples") ||
                                (boolean) o2.get("hasExamples")
                );

                return o1;
            });

            // Iterate through existing string, one character at a time.
            for (int i = 0; i < op.path.length(); i++) {
                switch (op.path.charAt(i)) {
                    case '{':
                        // We entered curly braces, so track that.
                        insideCurly++;

                        // Add the more complicated component instead of just the brace.
                        pathBuffer.append("${encodeURIComponent(String(_");
                        break;
                    case '}':
                        // We exited curly braces, so track that.
                        insideCurly--;

                        // Add the more complicated component instead of just the brace.
                        CodegenParameter parameter = findPathParameterByName(op, parameterName.toString());
                        pathBuffer.append(toParamName(parameterName.toString()));
                        if (parameter != null && parameter.isDateTime) {
                            pathBuffer.append(".toISOString()");
                        }
                        pathBuffer.append("))}");
                        parameterName.setLength(0);
                        break;
                    default:
                        char nextChar = op.path.charAt(i);
                        if (insideCurly > 0) {
                            parameterName.append(nextChar);
                        } else {
                            pathBuffer.append(nextChar);
                        }
                        break;
                }
            }

            // Overwrite path to TypeScript template string, after applying everything we just did.
            op.path = pathBuffer.toString();
        }

        operations.put("pathOperations", pathOperations.values());
        operations.put("hasSomeFormParams", hasSomeFormParams);

        // Add additional filename information for model imports in the services
        List<Map<String, Object>> imports = (List<Map<String, Object>>) operations.get("imports");
        for (Map<String, Object> im : imports) {
            // This property is not used in the templates any more, subject for removal
            im.put("filename", im.get("import"));
            im.put("classname", im.get("classname"));
        }

        return operations;
    }

    /**
     * Finds and returns a path parameter of an operation by its name
     *
     * @param operation     the operation
     * @param parameterName the name of the parameter
     * @return param
     */
    private CodegenParameter findPathParameterByName(CodegenOperation operation, String parameterName) {
        for (CodegenParameter param : operation.pathParams) {
            if (param.baseName.equals(parameterName)) {
                return param;
            }
        }
        return null;
    }

    @Override
    public Map<String, Object> postProcessModels(Map<String, Object> objs) {
        Map<String, Object> result = super.postProcessModels(objs);
        return postProcessModelsEnum(result);
    }

    @Override
    public Map<String, Object> postProcessAllModels(Map<String, Object> objs) {
        Map<String, Object> result = super.postProcessAllModels(objs);
        for (Map.Entry<String, Object> entry : result.entrySet()) {
            Map<String, Object> inner = (Map<String, Object>) entry.getValue();
            List<Map<String, Object>> models = (List<Map<String, Object>>) inner.get("models");
            for (Map<String, Object> mo : models) {
                CodegenModel cm = (CodegenModel) mo.get("model");
                if (taggedUnions) {
                    mo.put(TAGGED_UNIONS, true);
                    if (cm.discriminator != null && cm.children != null) {
                        for (CodegenModel child : cm.children) {
                            cm.imports.add(child.classname);
                        }
                    }
                    if (cm.parent != null) {
                        cm.imports.remove(cm.parent);
                    }
                }
                // Add additional filename information for imports
                Set<String> parsedImports = parseImports(cm);
                mo.put("tsImports", toTsImports(cm, parsedImports));
            }
        }
        return result;
    }

    /**
     * Parse imports
     */
    private Set<String> parseImports(CodegenModel cm) {
        Set<String> newImports = new HashSet<String>();
        if (cm.imports.size() > 0) {
            for (String name : cm.imports) {
                if (name.indexOf(" | ") >= 0) {
                    String[] parts = name.split(" \\| ");
                    for (String s : parts) {
                        newImports.add(s);
                    }
                } else {
                    newImports.add(name);
                }
            }
        }
        return newImports;
    }

    private List<Map<String, String>> toTsImports(CodegenModel cm, Set<String> imports) {
        List<Map<String, String>> tsImports = new ArrayList<>();
        for (String im : imports) {
            if (!im.equals(cm.classname)) {
                HashMap<String, String> tsImport = new HashMap<>();
                // TVG: This is used as class name in the import statements of the model file
                tsImport.put("classname", im);
                tsImport.put("filename", toModelFilename(removeModelPrefixSuffix(im)));
                tsImports.add(tsImport);
            }
        }
        return tsImports;
    }

    @Override
    public String toApiName(String name) {
        if (name.length() == 0) {
            return "DefaultService";
        }
        return camelize(name) + serviceSuffix;
    }

    @Override
    public String toApiFilename(String name) {
        if (name.length() == 0) {
            return "default.service";
        }
        return this.convertUsingFileNamingConvention(name) + serviceFileSuffix;
    }

    @Override
    public String toApiImport(String name) {
        if (importMapping.containsKey(name)) {
            return importMapping.get(name);
        }
        return apiPackage() + "/" + toApiFilename(name);
    }

    @Override
    public String toModelFilename(String name) {
        if (importMapping.containsKey(name)) {
            return importMapping.get(name);
        }
        return DEFAULT_IMPORT_PREFIX + this.convertUsingFileNamingConvention(this.sanitizeName(name)) + modelFileSuffix;
    }

    @Override
    public String toModelImport(String name) {
        if (importMapping.containsKey(name)) {
            return importMapping.get(name);
        }
        return modelPackage() + "/" + toModelFilename(name).substring(DEFAULT_IMPORT_PREFIX.length());
    }

    public String getNpmRepository() {
        return npmRepository;
    }

    public void setNpmRepository(String npmRepository) {
        this.npmRepository = npmRepository;
    }

    private boolean getUseSingleRequestParameter() {
        return useSingleRequestParameter;
    }

    private void setUseSingleRequestParameter(boolean useSingleRequestParameter) {
        this.useSingleRequestParameter = useSingleRequestParameter;
    }

    private String getApiFilenameFromClassname(String classname) {
        String name = classname.substring(0, classname.length() - serviceSuffix.length());
        return toApiFilename(name);
    }

    @Override
    public String toModelName(String name) {
        name = addSuffix(name, modelSuffix);
        return super.toModelName(name);
    }

    public String removeModelPrefixSuffix(String name) {
        String result = name;
        if (modelSuffix.length() > 0 && result.endsWith(modelSuffix)) {
            result = result.substring(0, result.length() - modelSuffix.length());
        }
        String prefix = capitalize(this.modelNamePrefix);
        String suffix = capitalize(this.modelNameSuffix);
        if (prefix.length() > 0 && result.startsWith(prefix)) {
            result = result.substring(prefix.length());
        }
        if (suffix.length() > 0 && result.endsWith(suffix)) {
            result = result.substring(0, result.length() - suffix.length());
        }

        return result;
    }

    /**
     * Validates that the given string value only contains '-', '.' and alpha numeric characters.
     * Throws an IllegalArgumentException, if the string contains any other characters.
     *
     * @param argument The name of the argument being validated. This is only used for displaying an error message.
     * @param value    The value that is being validated.
     */
    private void validateFileSuffixArgument(String argument, String value) {
        if (!value.matches(FILE_NAME_SUFFIX_PATTERN)) {
            throw new IllegalArgumentException(
                    String.format(Locale.ROOT, "%s file suffix only allows '.', '-' and alphanumeric characters.", argument)
            );
        }
    }

    /**
     * Validates that the given string value only contains alpha numeric characters.
     * Throws an IllegalArgumentException, if the string contains any other characters.
     *
     * @param argument The name of the argument being validated. This is only used for displaying an error message.
     * @param value    The value that is being validated.
     */
    private void validateClassPrefixArgument(String argument, String value) {
        if (!value.matches(CLASS_NAME_PREFIX_PATTERN)) {
            throw new IllegalArgumentException(
                    String.format(Locale.ROOT, "%s class prefix only allows alphanumeric characters.", argument)
            );
        }
    }

    /**
     * Validates that the given string value only contains alpha numeric characters.
     * Throws an IllegalArgumentException, if the string contains any other characters.
     *
     * @param argument The name of the argument being validated. This is only used for displaying an error message.
     * @param value    The value that is being validated.
     */
    private void validateClassSuffixArgument(String argument, String value) {
        if (!value.matches(CLASS_NAME_SUFFIX_PATTERN)) {
            throw new IllegalArgumentException(
                    String.format(Locale.ROOT, "%s class suffix only allows alphanumeric characters.", argument)
            );
        }
    }

    /**
     * Set the query param object format.
     *
     * @param format the query param object format to use
     */
    public void setQueryParamObjectFormat(String format) {
        try {
            queryParamObjectFormat = QUERY_PARAM_OBJECT_FORMAT_TYPE.valueOf(format);
        } catch (IllegalArgumentException e) {
            String values = Stream.of(QUERY_PARAM_OBJECT_FORMAT_TYPE.values())
                    .map(value -> "'" + value.name() + "'")
                    .collect(Collectors.joining(", "));

            String msg = String.format(Locale.ROOT, "Invalid query param object format '%s'. Must be one of %s.", format, values);
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Set the file naming type.
     *
     * @param fileNaming the file naming to use
     */
    private void setFileNaming(String fileNaming) {
        if ("camelCase".equals(fileNaming) || "kebab-case".equals(fileNaming)) {
            this.fileNaming = fileNaming;
        } else {
            throw new IllegalArgumentException("Invalid file naming '" +
                    fileNaming + "'. Must be 'camelCase' or 'kebab-case'");
        }
    }

    /**
     * Converts the original name according to the current <code>fileNaming</code> strategy.
     *
     * @param originalName the original name to transform
     * @return the transformed name
     */
    private String convertUsingFileNamingConvention(String originalName) {
        String name = this.removeModelPrefixSuffix(originalName);
        if ("kebab-case".equals(fileNaming)) {
            name = dashize(underscore(name));
        } else {
            name = camelize(name, true);
        }
        return name;
    }

    private CodegenParameter mapComponentRequestBody(Set<String> imports, java.util.Map.Entry<String, RequestBody> namedRequestBody) {
        String name = namedRequestBody.getKey();
        RequestBody requestBody = namedRequestBody.getValue();
        return fromRequestBody(requestBody, imports, name);
    }

    private CodegenParameter mapComponentParameter(Set<String> imports, java.util.Map.Entry<String, Parameter> nameParameter) {
        Parameter parameter = nameParameter.getValue();
        return fromParameter(parameter, imports);
    }

    private CodegenResponse mapCodegenResponse(java.util.Map.Entry<String, ApiResponse> codeResponse) {
        String responseCode = codeResponse.getKey();
        // try to resolve response code from key. otherwise use default
        responseCode = responseCode.replaceAll("\\D+", "");
        if (responseCode.length() != 3) {
            responseCode = "default";
        }
        ApiResponse response = codeResponse.getValue();
        return fromResponse(responseCode, response);
    }

    @Override
    public CodegenParameter fromParameter(Parameter parameter, Set<String> imports) {
        CodegenParameter codegenParameter = super.fromParameter(parameter, imports);
        log.debug("Created CodegenParameter model for parameter: {}", parameter.getName());
        return BoatCodegenParameter.fromCodegenParameter(parameter, codegenParameter, openAPI);
    }

    @Override
    public CodegenParameter fromRequestBody(RequestBody body, Set<String> imports, String bodyParameterName) {
        CodegenParameter codegenParameter = super.fromRequestBody(body, imports, bodyParameterName);
        log.debug("Created CodegenParameter model for request body: {} with bodyParameterName: {}", codegenParameter.baseName, bodyParameterName);
        return BoatCodegenParameter.fromCodegenParameter(codegenParameter, body, openAPI);
    }

    @Override
    public CodegenResponse fromResponse(String responseCode, ApiResponse response) {
        CodegenResponse r = super.fromResponse(responseCode, response);
        r.message = StringUtils.replace(r.message, "`", "\\`");

        return new BoatCodegenResponse(r, responseCode, response, openAPI);
    }

    public static enum QUERY_PARAM_OBJECT_FORMAT_TYPE {dot, json, key}

}
