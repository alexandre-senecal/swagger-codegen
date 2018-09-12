package io.swagger.codegen.languages;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import io.swagger.codegen.CliOption;
import io.swagger.codegen.CodegenConstants;
import io.swagger.codegen.CodegenModel;
import io.swagger.codegen.CodegenOperation;
import io.swagger.codegen.CodegenProperty;
import io.swagger.codegen.CodegenType;
import io.swagger.codegen.SupportingFile;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import io.swagger.util.Json;

public class JavaJAXRSForRestyGWTClientCodegen extends AbstractJavaJAXRSServerCodegen {
	public static final String GENERATE_POM = "generatePom";
	public static final String GWT_MODULE_NAME = "gwtModuleName";
	public static final String GWT_MODULE_PACKAGE = "gwtModulePackage";
	
	private boolean generatePom = true;
	private String gwtModuleName = "SwaggerRestyGwtClient";
	private String gwtModulePackage = "io.swagger";

	public JavaJAXRSForRestyGWTClientCodegen() {
		super();
		invokerPackage = "io.swagger.api";
		artifactId = "swagger-jaxrs-restygwt";
		outputFolder = "generated-code/JavaJaxRS-restygwt";

		apiTemplateFiles.put("api.mustache", ".java");
		apiPackage = "io.swagger.client.api";
		modelPackage = "io.swagger.shared.model";

		apiTestTemplateFiles.clear(); // TODO: add api test template
		modelTestTemplateFiles.clear(); // TODO: add model test template

		// clear model and api doc template as this codegen
		// does not support auto-generated markdown doc at the moment
		// TODO: add doc templates
		modelDocTemplateFiles.remove("model_doc.mustache");
		apiDocTemplateFiles.remove("api_doc.mustache");

		additionalProperties.put("title", title);

		typeMapping.put("date", "LocalDate");

		importMapping.put("LocalDate", "org.joda.time.LocalDate");

		super.embeddedTemplateDir = templateDir = JAXRS_TEMPLATE_DIRECTORY_NAME + File.separator + "restygwt";

		for (int i = 0; i < cliOptions.size(); i++) {
			if (CodegenConstants.LIBRARY.equals(cliOptions.get(i).getOpt())) {
				cliOptions.remove(i);
				break;
			}
		}

		CliOption library = new CliOption(CodegenConstants.LIBRARY, "library template (sub-template) to use");
		library.setDefault(DEFAULT_LIBRARY);

		Map<String, String> supportedLibraries = new LinkedHashMap<String, String>();

		supportedLibraries.put(DEFAULT_LIBRARY, "JAXRS");
		library.setEnum(supportedLibraries);

		additionalProperties.put("gwtModuleName", gwtModuleName);
		
		cliOptions.add(library);
		cliOptions.add(
				CliOption.newBoolean(GENERATE_POM, "Whether to generate pom.xml if the file does not already exist.")
						.defaultValue(String.valueOf(generatePom)));
		cliOptions.add(
				CliOption.newString(GWT_MODULE_NAME, "The name of the GWT module")
						.defaultValue(String.valueOf(gwtModuleName)));
		cliOptions.add(
				CliOption.newString(GWT_MODULE_PACKAGE, "The package of the GWT module")
						.defaultValue(String.valueOf(gwtModulePackage)));
	}

	@Override
	public void processOpts() {
		if (additionalProperties.containsKey(GENERATE_POM)) {
			generatePom = Boolean.valueOf(additionalProperties.get(GENERATE_POM).toString());
		}
		if (additionalProperties.containsKey(GWT_MODULE_NAME)) {
			gwtModuleName = (String) additionalProperties.get(GWT_MODULE_NAME);
		}
		if (additionalProperties.containsKey(GWT_MODULE_PACKAGE)) {
			gwtModuleName = (String) additionalProperties.get(GWT_MODULE_PACKAGE);
		}
		
		super.processOpts();
		supportingFiles.clear(); // Don't need extra files provided by AbstractJAX-RS & Java Codegen
		if (generatePom) {
			writeOptional(outputFolder, new SupportingFile("pom.mustache", "", "pom.xml"));
		}
		writeOptional(outputFolder, new SupportingFile("README.mustache", "", "README.md"));
		writeOptional(outputFolder, new SupportingFile("gwtmodule.mustache",
				(sourceFolder + '/' + gwtModulePackage).replace(".", "/"), gwtModuleName + ".gwt.xml"));
	}

	@Override
	public String getName() {
		return "jaxrs-restygwt";
	}

	@Override
	public CodegenType getTag() {
		return CodegenType.CLIENT;
	}

	@Override
	public void addOperationToGroup(String tag, String resourcePath, Operation operation, CodegenOperation co,
			Map<String, List<CodegenOperation>> operations) {
		String basePath = resourcePath;
		if (basePath.startsWith("/")) {
			basePath = basePath.substring(1);
		}
		int pos = basePath.indexOf("/");
		if (pos > 0) {
			basePath = basePath.substring(0, pos);
		}

		if (basePath == "") {
			basePath = "default";
		} else {
			if (co.path.startsWith("/" + basePath)) {
				co.path = co.path.substring(("/" + basePath).length());
			}
			co.subresourceOperation = !co.path.isEmpty();
		}
		List<CodegenOperation> opList = operations.get(basePath);
		if (opList == null) {
			opList = new ArrayList<CodegenOperation>();
			operations.put(basePath, opList);
		}
		opList.add(co);
		co.baseName = basePath;
	}

	@Override
	public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
		super.postProcessModelProperty(model, property);
		model.imports.remove("ApiModelProperty");
		model.imports.remove("ApiModel");
		model.imports.remove("JsonSerialize");
		model.imports.remove("ToStringSerializer");
		model.imports.remove("JsonValue");
		model.imports.remove("JsonProperty");
	}

	@Override
	public void preprocessSwagger(Swagger swagger) {
		// copy input swagger to output folder
		try {
			String swaggerJson = Json.pretty(swagger);
			FileUtils.writeStringToFile(new File(outputFolder + File.separator + "swagger.json"), swaggerJson);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e.getCause());
		}
		super.preprocessSwagger(swagger);

	}

	@Override
	public String getHelp() {
		return "Generates a Java JAXRS Interace according to JAXRS 2.0 specification and compatible with RestyGWT.";
	}

	@Override
	public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
		super.postProcessOperations(objs);

		@SuppressWarnings("unchecked")
		Map<String, Object> operations = (Map<String, Object>) objs.get("operations");
		if (operations != null) {
			@SuppressWarnings("unchecked")
			List<CodegenOperation> ops = (List<CodegenOperation>) operations.get("operation");
			for (CodegenOperation operation : ops) {
				if (operation.returnType == "void") {
					operation.returnType = "Void";
				}
			}
		}
		return objs;
	}

}
