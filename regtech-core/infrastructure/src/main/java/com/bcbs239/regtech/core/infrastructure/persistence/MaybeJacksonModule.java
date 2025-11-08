package com.bcbs239.regtech.core.infrastructure.persistence;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;

/**
 * Jackson module to (de)serialize Maybe<T> values.
 * - Some(value) -> value
 * - None -> null
 */
public class MaybeJacksonModule extends SimpleModule {
    public MaybeJacksonModule() {
        super("MaybeModule");
    // Use raw-cast to satisfy the generic signature
    @SuppressWarnings({"unchecked","rawtypes"})
    Class<? extends Maybe<?>> maybeClass = (Class) Maybe.class;
    addSerializer((Class) maybeClass, (JsonSerializer) new MaybeSerializer());
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
}
