package com.speech.protocol;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Data
public class Message {
    private byte version = VersionBits.Version1.getValue();
    private byte headerSize = HeaderSizeBits.HeaderSize4.getValue();
    private MsgType type;
    private MsgTypeFlagBits flag;
    private byte serialization = SerializationBits.JSON.getValue();
    private byte compression = 0;

    private EventType event;
    private String sessionId;
    private String connectId;
    private int sequence;
    private int errorCode;

    private byte[] payload;

    public Message(MsgType type, MsgTypeFlagBits flag) {
        this.type = type;
        this.flag = flag;
    }

    public static Message unmarshal(byte[] data) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(data);

        byte type_and_flag = data[1];
        MsgType type = MsgType.fromValue((type_and_flag >> 4) & 0x0F);
        MsgTypeFlagBits flag = MsgTypeFlagBits.fromValue(type_and_flag & 0x0F);

        // Read version and header size
        int versionAndHeaderSize = buffer.get();
        VersionBits version = VersionBits.fromValue((versionAndHeaderSize >> 4) & 0x0F);
        HeaderSizeBits headerSize = HeaderSizeBits.fromValue(versionAndHeaderSize & 0x0F);

        // Skip second byte
        buffer.get();

        // Read serialization and compression method
        int serializationCompression = buffer.get();
        SerializationBits serialization = SerializationBits.fromValue((serializationCompression >> 4) & 0x0F);
        CompressionBits compression = CompressionBits.fromValue(serializationCompression & 0x0F);

        // Skip padding bytes
        int headerSizeInt = 4 * (int) headerSize.getValue();
        int paddingSize = headerSizeInt - 3;
        while (paddingSize > 0) {
            buffer.get();
            paddingSize -= 1;
        }

        Message message = new Message(type, flag);
        message.setVersion(version.getValue());
        message.setHeaderSize(headerSize.getValue());
        message.setSerialization(serialization.getValue());
        message.setCompression(compression.getValue());

        // Read sequence if present
        if (flag == MsgTypeFlagBits.POSITIVE_SEQ || flag == MsgTypeFlagBits.NEGATIVE_SEQ) {
            // Read 4 bytes from ByteBuffer and parse as int (big-endian)
            byte[] sequeueBytes = new byte[4];
            if (buffer.remaining() >= 4) {
                buffer.get(sequeueBytes); // Read 4 bytes into array
                ByteBuffer wrapper = ByteBuffer.wrap(sequeueBytes);
                wrapper.order(ByteOrder.BIG_ENDIAN); // Set big-endian order
                message.setSequence(wrapper.getInt());
            }
        }

        // Read event if present
        if (flag == MsgTypeFlagBits.WITH_EVENT) {
            // Read 4 bytes from ByteBuffer and parse as int (big-endian)
            byte[] eventBytes = new byte[4];
            if (buffer.remaining() >= 4) {
                buffer.get(eventBytes); // Read 4 bytes into array
                ByteBuffer wrapper = ByteBuffer.wrap(eventBytes);
                wrapper.order(ByteOrder.BIG_ENDIAN); // Set big-endian order
                message.setEvent(EventType.fromValue(wrapper.getInt()));
            }

            if (type != MsgType.ERROR && !(message.event == EventType.START_CONNECTION
                    || message.event == EventType.FINISH_CONNECTION ||
                    message.event == EventType.CONNECTION_STARTED
                    || message.event == EventType.CONNECTION_FAILED ||
                    message.event == EventType.CONNECTION_FINISHED)) {
                // Read sessionId if present
                int sessionIdLength = buffer.getInt();
                if (sessionIdLength > 0) {
                    byte[] sessionIdBytes = new byte[sessionIdLength];
                    buffer.get(sessionIdBytes);
                    message.setSessionId(new String(sessionIdBytes, StandardCharsets.UTF_8));
                }
            }

            if (message.event == EventType.CONNECTION_STARTED || message.event == EventType.CONNECTION_FAILED
                    || message.event == EventType.CONNECTION_FINISHED) {
                // Read connectId if present
                int connectIdLength = buffer.getInt();
                if (connectIdLength > 0) {
                    byte[] connectIdBytes = new byte[connectIdLength];
                    buffer.get(connectIdBytes);
                    message.setConnectId(new String(connectIdBytes, StandardCharsets.UTF_8));
                }
            }
        }

        // Read errorCode if present
        if (type == MsgType.ERROR) {
            // Read 4 bytes from ByteBuffer and parse as int (big-endian)
            byte[] errorCodeBytes = new byte[4];
            if (buffer.remaining() >= 4) {
                buffer.get(errorCodeBytes); // Read 4 bytes into array
                ByteBuffer wrapper = ByteBuffer.wrap(errorCodeBytes);
                wrapper.order(ByteOrder.BIG_ENDIAN); // Set big-endian order
                message.setErrorCode(wrapper.getInt());
            }
        }

        // Read remaining bytes as payload
        if (buffer.remaining() > 0) {
            // 4 bytes length
            int payloadLength = buffer.getInt();
            if (payloadLength > 0) {
                byte[] payloadBytes = new byte[payloadLength];
                buffer.get(payloadBytes);
                message.setPayload(payloadBytes);
            }
        }

        return message;
    }

    public byte[] marshal() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        // Write header
        buffer.write((version & 0x0F) << 4 | (headerSize & 0x0F));
        buffer.write((type.getValue() & 0x0F) << 4 | (flag.getValue() & 0x0F));
        buffer.write((serialization & 0x0F) << 4 | (compression & 0x0F));

        int headerSizeInt = 4 * (int) headerSize;
        int padding = headerSizeInt - buffer.size();
        while (padding > 0) {
            buffer.write(0);
            padding -= 1;
        }

        // Write event if present
        if (event != null) {
            byte[] eventBytes = ByteBuffer.allocate(4).putInt(event.getValue()).array();
            buffer.write(eventBytes);
        }

        // Write sessionId if present
        if (sessionId != null) {
            byte[] sessionIdBytes = sessionId.getBytes(StandardCharsets.UTF_8);
            buffer.write(ByteBuffer.allocate(4).putInt(sessionIdBytes.length).array());
            buffer.write(sessionIdBytes);
        }

        // Write connectId if present
        if (connectId != null) {
            byte[] connectIdBytes = connectId.getBytes(StandardCharsets.UTF_8);
            buffer.write(ByteBuffer.allocate(4).putInt(connectIdBytes.length).array());
            buffer.write(connectIdBytes);
        }

        // Write sequence if present
        if (sequence != 0) {
            buffer.write(ByteBuffer.allocate(4).putInt(sequence).array());
        }

        // Write errorCode if present
        if (errorCode != 0) {
            buffer.write(ByteBuffer.allocate(4).putInt(errorCode).array());
        }

        // Write payload if present
        if (payload != null && payload.length > 0) {
            buffer.write(ByteBuffer.allocate(4).putInt(payload.length).array());
            buffer.write(payload);
        }
        return buffer.toByteArray();
    }

    @Override
    public String toString() {
        switch (this.type) {
            case AUDIO_ONLY_SERVER:
            case AUDIO_ONLY_CLIENT:
                if (this.flag == MsgTypeFlagBits.POSITIVE_SEQ || this.flag == MsgTypeFlagBits.NEGATIVE_SEQ) {
                    return String.format("MsgType: %s, EventType: %s, Sequence: %d, PayloadSize: %d", this.type, this.event, this.sequence,
                            this.payload != null ? this.payload.length : 0);
                }
                return String.format("MsgType: %s, EventType: %s, PayloadSize: %d", this.type, this.event,
                        this.payload != null ? this.payload.length : 0);
            case ERROR:
                return String.format("MsgType: %s, EventType: %s, ErrorCode: %d, Payload: %s", this.type, this.event, this.errorCode,
                        this.payload != null ? new String(this.payload) : "null");
            default:
                if (this.flag == MsgTypeFlagBits.POSITIVE_SEQ || this.flag == MsgTypeFlagBits.NEGATIVE_SEQ) {
                    return String.format("MsgType: %s, EventType: %s, Sequence: %d, Payload: %s",
                            this.type, this.event, this.sequence,
                            this.payload != null ? new String(this.payload) : "null");
                }
                return String.format("MsgType: %s, EventType: %s, Payload: %s", this.type, this.event,
                        this.payload != null ? new String(this.payload) : "null");
        }
    }
}