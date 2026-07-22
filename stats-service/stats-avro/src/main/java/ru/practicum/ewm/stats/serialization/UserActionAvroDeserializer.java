package ru.practicum.ewm.stats.serialization;

import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.io.IOException;

public class UserActionAvroDeserializer implements Deserializer<UserActionAvro> {

    @Override
    public UserActionAvro deserialize(
            String topic,
            byte[] data
    ) {
        if (data == null) {
            return null;
        }

        try {
            SpecificDatumReader<UserActionAvro> reader =
                    new SpecificDatumReader<>(
                            UserActionAvro.getClassSchema()
                    );

            return reader.read(
                    null,
                    DecoderFactory.get()
                            .binaryDecoder(data, null)
            );

        } catch (IOException exception) {
            throw new SerializationException(
                    "Failed to deserialize UserActionAvro "
                            + "from topic "
                            + topic,
                    exception
            );
        }
    }
}