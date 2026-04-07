package tech.kayys.wayang.prompt.node;

public class PromptSchemas {
    public static final String PROMPT_RENDER_CONFIG = """
            {
              "type": "object",
              "properties": {
                "templateRefs": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "id": {
                        "type": "string",
                        "description": "Template ID"
                      },
                      "version": {
                        "type": "string",
                        "description": "Template Version (leave empty for latest)"
                      }
                    },
                    "required": ["id"]
                  },
                  "description": "List of templates to render in order"
                }
              },
              "required": ["templateRefs"]
            }
            """;
}
