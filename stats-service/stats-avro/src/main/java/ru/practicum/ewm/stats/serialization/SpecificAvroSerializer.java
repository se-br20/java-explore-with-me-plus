package ru.practicum.ewm.stats.serialization;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SpecificAvroSerializer<T extends SpecificRecordBase> implements Serializer<T> {

    @Override
    public byte[] serialize(
            String topic,
            T data
    ) {
        if (data == null) {
            return null;
        }

        try (ByteArrayOutputStream outputStream =
                     new ByteArrayOutputStream()) {

            DatumWriter<T> writer =
                    new SpecificDatumWriter<>(
                            data.getSchema()
                    );

            BinaryEncoder encoder =
                    EncoderFactory.get()
                            .directBinaryEncoder(
                                    outputStream,
                                    null
                            );

            writer.write(data, encoder);
            encoder.flush();

            return outputStream.toByteArray();

        } catch (IOException exception) {
            throw new SerializationException(
                    "Failed to serialize Avro message "
                            + "for topic "
                            + topic,
                    exception
            );
        }
    }
}