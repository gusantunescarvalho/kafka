package com.test.kafka.topics;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.RetryTopicHeaders;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.converter.ConversionException;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class KafkaConsumer {

    @RetryableTopic(
            attempts = "6",
            backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            exclude = {
                    DeserializationException.class,
                    SerializationException.class,
                    MessageConversionException.class,
                    ConversionException.class,
                    MethodArgumentResolutionException.class,
                    NoSuchMethodException.class,
                    ClassCastException.class
            }
    )
    @KafkaListener(topics = "newTopic", groupId = "grp_a")
    public void listenTestTopic(ConsumerRecord<String, String> message) {

        Header header = message.headers().lastHeader(RetryTopicHeaders.DEFAULT_HEADER_ATTEMPTS);

        String topic = message.topic();
        LocalDateTime dateTime = LocalDateTime.now();
        System.out.println("Received message from topic: " + topic + " message: " +  message.value() + " - " + dateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")) + " header: " +header);
        if (message.value().startsWith("foo")) {
            throw new RecoverableDataAccessException("Recoverable");
        } else if (message.value().startsWith("bar")) {
            throw new ClassCastException("Unrecoverable");
        }
    }

}
