package com.bcbs239.regtech.core.infrastructure.persistence;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.JavaType;

import java.io.IOException;

/**
 * Jackson module to (de)serialize Maybe<T> values.
 * - Serialization: Some(value) -> value, None -> null
 * - Deserialization: null -> None, value -> Some(value)
 */
public class MaybeJacksonModule extends SimpleModule {
    public MaybeJacksonModule() {
        super("MaybeModule");
        // Use raw-cast to satisfy the generic signature
        @SuppressWarnings({"unchecked","rawtypes"})
        Class<? extends Maybe<?>> maybeClass = (Class) Maybe.class;
        addSerializer((Class) maybeClass, (JsonSerializer) new MaybeSerializer());
        addDeserializer((Class) maybeClass, (JsonDeserializer) new MaybeDeserializer());
    }

    private static class MaybeSerializer extends JsonSerializer<Maybe<?>> {
        @Override
        public void serialize(Maybe<?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }
            try {
                if (value.isEmpty()) {
                    gen.writeNull();
                } else {
                    Object inner = value.getValue();
                    // Delegate serialization of inner value to default serializer
                    serializers.defaultSerializeValue(inner, gen);
                }
            } catch (RuntimeException ex) {
                // If getValue throws unexpectedly, write null to avoid breaking persistence
                gen.writeNull();
            }
        }
    }

    private static class MaybeDeserializer extends JsonDeserializer<Maybe<?>> {
        @Override
        public Maybe<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            // Check if the current token is null
            if (p.currentToken() == null || p.currentToken().isStructEnd()) {
                return Maybe.none();
            }
            
            // Check for explicit null value
            if (p.currentToken().isScalarValue()) {
                String text = p.getText();
                if (text == null || "null".equals(text)) {
                    return Maybe.none();
                }
            }
            
            // Get the type parameter from context if available
            JavaType contextualType = ctxt.getContextualType();
            if (contextualType != null && contextualType.containedTypeCount() > 0) {
                // Maybe<String>, Maybe<Integer>, etc.
                JavaType innerType = contextualType.containedType(0);
                Object value = ctxt.readValue(p, innerType);
                if (value == null) {
                    return Maybe.none();
                }
                return Maybe.some(value);
            }
            
            // Fallback: try to read as Object
            Object value = ctxt.readValue(p, Object.class);
            if (value == null) {
                return Maybe.none();
            }
            return Maybe.some(value);
        }
    }
}
