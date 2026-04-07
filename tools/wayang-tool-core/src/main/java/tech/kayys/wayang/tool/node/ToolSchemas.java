package tech.kayys.wayang.tool.node;

/**
 * Common JSON schemas for tool nodes.
 */
public final class ToolSchemas {

    public static final String TOOL_HTTP_CONFIG = """
        {
          "$schema": "http://json-schema.org/draft-07/schema#",
          "type": "object",
          "properties": {
            "url": {
              "type": "string",
              "description": "The URL to call."
            },
            "method": {
              "type": "string",
              "enum": ["GET", "POST", "PUT", "DELETE", "PATCH"],
              "default": "GET",
              "description": "HTTP method."
            },
            "headers": {
              "type": "object",
              "description": "HTTP headers."
            },
            "queryParameters": {
              "type": "object",
              "description": "URL query parameters."
            },
            "body": {
              "type": "string",
              "description": "HTTP request body (for POST/PUT/PATCH)."
            },
            "timeout": {
              "type": "integer",
              "minimum": 1,
              "default": 30,
              "description": "Timeout in seconds."
            }
          },
          "required": ["url"]
        }
        """;

    public static final String TOOL_SANDBOX_CONFIG = """
        {
          "$schema": "http://json-schema.org/draft-07/schema#",
          "type": "object",
          "properties": {
            "code": {
              "type": "string",
              "description": "The code to execute."
            },
            "language": {
              "type": "string",
              "enum": ["python", "javascript", "bash"],
              "default": "python",
              "description": "The programming language."
            },
            "requirements": {
              "type": "array",
              "items": { "type": "string" },
              "description": "List of dependencies/requirements."
            },
            "timeout": {
              "type": "integer",
              "minimum": 1,
              "default": 60,
              "description": "Execution timeout in seconds."
            }
          },
          "required": ["code"]
        }
        """;

    public static final String TOOL_INVOCATION_CONFIG = """
        {
          "$schema": "http://json-schema.org/draft-07/schema#",
          "type": "object",
          "properties": {
            "toolId": {
              "type": "string",
              "description": "Unique identifier of the tool to invoke."
            },
            "arguments": {
              "type": "object",
              "description": "Arguments to pass to the tool."
            }
          },
          "required": ["toolId"]
        }
        """;

    public static final String TOOL_REST_CONFIG = """
        {
          "$schema": "http://json-schema.org/draft-07/schema#",
          "type": "object",
          "properties": {
            "baseUrl": {
              "type": "string",
              "description": "The base URL for the REST API."
            },
            "path": {
              "type": "string",
              "description": "The specific endpoint path."
            },
            "method": {
              "type": "string",
              "enum": ["GET", "POST", "PUT", "DELETE", "PATCH"],
              "default": "GET",
              "description": "HTTP method."
            },
            "headers": {
              "type": "object",
              "description": "HTTP headers."
            },
            "queryParams": {
              "type": "object",
              "description": "URL query parameters."
            },
            "body": {
              "type": "object",
              "description": "JSON request body."
            },
            "timeout": {
              "type": "integer",
              "minimum": 1,
              "default": 30,
              "description": "Timeout in seconds."
            }
          },
          "required": ["baseUrl", "path"]
        }
        """;

    private ToolSchemas() {}
}
