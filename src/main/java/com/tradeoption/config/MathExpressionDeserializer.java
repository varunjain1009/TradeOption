package com.tradeoption.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class MathExpressionDeserializer extends JsonDeserializer<Long> {

    @Override
    public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        if (node.isNumber()) {
            return node.asLong();
        } else if (node.isTextual()) {
            return evaluate(node.asText());
        }
        return 0L;
    }

    private Long evaluate(String expression) {
        // Simple evaluation support: *, +, -, /
        // Prioritize multiplication/division over addition/subtraction
        // However, for simplicity and the requirement, we can handle simple cases.
        // Let's use a simpler approach: Evaluate strictly left-to-right (or use JS
        // engine if needed, but Java is safer).
        // Actually, user example: "15*60*1000". This is pure multiplication.

        try {
            // Remove spaces
            expression = expression.replaceAll("\\s+", "");

            // Only supporting strict multiplication for now as per requirement example
            // "15*60*1000"
            // If more complex math is needed, we should use a library or a proper parser.
            // For now, let's split by '*' and multiply.
            if (expression.contains("*")) {
                String[] parts = expression.split("\\*");
                long result = 1;
                for (String part : parts) {
                    result *= Long.parseLong(part);
                }
                return result;
            } else {
                return Long.parseLong(expression);
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid math expression for config: " + expression, e);
        }
    }
}
