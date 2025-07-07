package com.example.demo.tools;

import com.example.demo.schema.JsonSchema;
import com.example.demo.schema.SchemaValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;

import java.util.concurrent.CompletableFuture;

/**
 * Base abstract class for tools
 * All tools should inherit from this class
 */
public abstract class BaseTool<P> {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    protected final String name;
    protected final String displayName;
    protected final String description;
    protected final JsonSchema parameterSchema;
    protected final boolean isOutputMarkdown;
    protected final boolean canUpdateOutput;
    
    protected SchemaValidator schemaValidator;

    public BaseTool(String name, String displayName, String description, JsonSchema parameterSchema) {
        this(name, displayName, description, parameterSchema, true, false);
    }

    public BaseTool(String name, String displayName, String description, JsonSchema parameterSchema,
                   boolean isOutputMarkdown, boolean canUpdateOutput) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.parameterSchema = parameterSchema;
        this.isOutputMarkdown = isOutputMarkdown;
        this.canUpdateOutput = canUpdateOutput;
    }

    /**
     * Set Schema validator (through dependency injection)
     */
    public void setSchemaValidator(SchemaValidator schemaValidator) {
        this.schemaValidator = schemaValidator;
    }

    /**
     * Validate tool parameters
     *
     * @param params Parameter object
     * @return Validation error message, null means validation passed
     */
    public String validateToolParams(P params) {
        if (schemaValidator == null || parameterSchema == null) {
            logger.warn("Schema validator or parameter schema is null, skipping validation");
            return null;
        }
        
        try {
            return schemaValidator.validate(parameterSchema, params);
        } catch (Exception e) {
            logger.error("Parameter validation failed", e);
            return "Parameter validation error: " + e.getMessage();
        }
    }

    /**
     * Confirm whether user approval is needed for execution
     *
     * @param params Parameter object
     * @return Confirmation details, null means no confirmation needed
     */
    public CompletableFuture<ToolConfirmationDetails> shouldConfirmExecute(P params) {
        return CompletableFuture.completedFuture(null); // Default no confirmation needed
    }

    /**
     * Execute tool
     *
     * @param params Parameter object
     * @return Execution result
     */
    public abstract CompletableFuture<ToolResult> execute(P params);

    /**
     * Get tool description (for AI understanding)
     *
     * @param params Parameter object
     * @return Description information
     */
    public String getDescription(P params) {
        return description;
    }

    // Getters
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public JsonSchema getParameterSchema() { return parameterSchema; }
    public boolean isOutputMarkdown() { return isOutputMarkdown; }
    public boolean canUpdateOutput() { return canUpdateOutput; }

    @Override
    public String toString() {
        return String.format("Tool{name='%s', displayName='%s'}", name, displayName);
    }
}
