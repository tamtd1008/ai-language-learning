package com.example.backend.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Iterator;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared request and validation path for features that require structured AI output. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiJsonHelper {

    private final AIProvider aiProvider;

    private final ObjectMapper objectMapper;

    public <T> T requestJson(
            String systemPrompt,
            String userMessage,
            Double temperature,
            Class<T> resultType
    ) {

        AiChatResponse response = aiProvider.chat(
                AiChatRequest.builder()
                        .systemPrompt(systemPrompt)
                        .messages(List.of(
                                AiChatMessage.builder()
                                        .role(AiChatRole.USER)
                                        .content(userMessage)
                                        .build()
                        ))
                        .temperature(temperature)
                        .maxTokens(256)
                        .jsonMode(true)
                        .jsonSchema(buildSchema(resultType))
                        .build()
        );

        if (response == null || response.getContent() == null) {
            throw new AIProviderException("AI returned an empty structured response.");
        }

        String cleaned = stripMarkdownFence(response.getContent());

        try {
            T parsed = objectMapper.readValue(cleaned, resultType);

            if (parsed == null || containsMissingValue(objectMapper.valueToTree(parsed))) {
                log.warn(
                        "AI returned incomplete structured data (type={}, model={}, responseChars={})",
                        resultType.getSimpleName(), response.getModel(), cleaned.length()
                );
                throw new AIProviderException(
                        "AI returned incomplete structured data. Please retry."
                );
            }

            return parsed;
        } catch (JacksonException e) {
            log.warn(
                    "AI response was not valid JSON (type={}, model={}, responseChars={})",
                    resultType.getSimpleName(), response.getModel(), cleaned.length()
            );
            throw new AIProviderException(
                    "AI returned an invalid structured response. Please retry.", e
            );
        }
    }

    private boolean containsMissingValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return true;
        }

        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.properties().iterator();
            while (fields.hasNext()) {
                if (containsMissingValue(fields.next().getValue())) {
                    return true;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode value : node) {
                if (containsMissingValue(value)) {
                    return true;
                }
            }
        }

        return false;
    }

    private Map<String, Object> buildSchema(Class<?> type) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }

            JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
            String name = jsonProperty != null && !jsonProperty.value().isBlank()
                    ? jsonProperty.value()
                    : field.getName();
            Map<String, Object> propertySchema = schemaFor(field.getGenericType());
            Size size = field.getAnnotation(Size.class);
            if (size != null && "array".equals(propertySchema.get("type"))) {
                if (size.min() > 0) {
                    propertySchema.put("minItems", size.min());
                }
                if (size.max() < Integer.MAX_VALUE) {
                    propertySchema.put("maxItems", size.max());
                }
            }
            properties.put(name, propertySchema);
            required.add(name);
        }

        schema.put("properties", properties);
        schema.put("required", required);
        return schema;
    }

    private Map<String, Object> schemaFor(Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            Type rawType = parameterizedType.getRawType();
            if (rawType instanceof Class<?> rawClass
                    && Iterable.class.isAssignableFrom(rawClass)) {
                Map<String, Object> schema = new LinkedHashMap<>();
                schema.put("type", "array");
                schema.put("items", schemaFor(parameterizedType.getActualTypeArguments()[0]));
                return schema;
            }
        }

        if (!(type instanceof Class<?> javaType)) {
            return Map.of("type", "string");
        }
        if (javaType == String.class || javaType.isEnum()) {
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("type", "string");
            if (javaType.isEnum()) {
                schema.put("enum", java.util.Arrays.stream(javaType.getEnumConstants())
                        .map(Object::toString).toList());
            }
            return schema;
        }
        if (javaType == boolean.class || javaType == Boolean.class) {
            return Map.of("type", "boolean");
        }
        if (javaType == byte.class || javaType == short.class
                || javaType == int.class || javaType == long.class
                || javaType == Byte.class || javaType == Short.class
                || javaType == Integer.class || javaType == Long.class) {
            return Map.of("type", "integer");
        }
        if (Number.class.isAssignableFrom(javaType)
                || javaType == float.class || javaType == double.class) {
            return Map.of("type", "number");
        }
        return buildSchema(javaType);
    }

    private String stripMarkdownFence(String content) {
        String trimmed = content == null ? "" : content.trim();

        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }

        return trimmed.trim();
    }
}
