package ru.practicum.ewm.stats.serialization;

import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.io.IOException;

public class EventSimilarityAvroDeserializer implements Deserializer<EventSimilarityAvro> {

    @Override
    public EventSimilarityAvro deserialize(
            String topic,
            byte[] data
    ) {
        if (data == null) {
            return null;
        }

        try {
            SpecificDatumReader<EventSimilarityAvro> reader =
                    new SpecificDatumReader<>(
                            EventSimilarityAvro.getClassSchema()
                    );

            return reader.read(
                    null,
                    DecoderFactory.get()
                            .binaryDecoder(data, null)
            );

        } catch (IOException exception) {
            throw new SerializationException(
                    "Failed to deserialize "
                            + "EventSimilarityAvro from topic "
                            + topic,
                    exception
            );
        }
    }
}