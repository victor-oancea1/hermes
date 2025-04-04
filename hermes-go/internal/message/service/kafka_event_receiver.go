package service

import (
	"context"
	"fmt"
	"strings"
	"time"

	"github.com/confluentinc/confluent-kafka-go/v2/kafka"
	"github.com/victor-oancea1/hermes/hermes-go/internal/application/json"
	"github.com/victor-oancea1/hermes/hermes-go/internal/domain"
)

type KafkaEventReceiver struct {
	receiverFactory func(domain.ReceiverOption) *kafka.Consumer
}

func NewKafkaEventReceiver(receiverFactory func(domain.ReceiverOption) *kafka.Consumer) *KafkaEventReceiver {
	return &KafkaEventReceiver{
		receiverFactory: receiverFactory,
	}
}

func (k *KafkaEventReceiver) Receive(ctx context.Context, receiverOptions []domain.ReceiverOption) (<-chan domain.Event, error) {
	eventChan := make(chan domain.Event)

	for _, option := range receiverOptions {
		consumer := k.receiverFactory(option)
		
		var err error
		switch option.Offset.OffsetType {
		case domain.OffsetTypeStart:
			err = consumer.Assign([]kafka.TopicPartition{
				{
					Topic:     &option.Topic,
					Partition: 0,
					Offset:    kafka.OffsetBeginning,
				},
			})
		case domain.OffsetTypeEnd:
			err = consumer.Assign([]kafka.TopicPartition{
				{
					Topic:     &option.Topic,
					Partition: 0,
					Offset:    kafka.OffsetEnd,
				},
			})
		case domain.OffsetTypeSpecific:
			if option.Offset.Value == nil {
				return nil, fmt.Errorf("specific offset requires a value")
			}
			err = consumer.Assign([]kafka.TopicPartition{
				{
					Topic:     &option.Topic,
					Partition: 0,
					Offset:    kafka.Offset(int64(*option.Offset.Value)),
				},
			})
		}

		if err != nil {
			return nil, fmt.Errorf("failed to assign topic: %w", err)
		}

		go func(consumer *kafka.Consumer, option domain.ReceiverOption) {
			defer consumer.Close()
			
			count := 0
			for {
				select {
				case <-ctx.Done():
					return
				default:
					msg, err := consumer.ReadMessage(100 * time.Millisecond)
					if err != nil {
						continue
					}

					event, err := messageToEvent(msg)
					if err != nil {
						fmt.Printf("Error converting message to event: %v\n", err)
						continue
					}

					if option.Expression != "*" && !strings.Contains(event.Key, option.Expression) {
						continue
					}

					eventChan <- event

					count++
					if count >= option.Limit {
						return
					}
				}
			}
		}(consumer, option)
	}

	return eventChan, nil
}

func messageToEvent(msg *kafka.Message) (domain.Event, error) {
	headers := make(map[string]string)
	for _, header := range msg.Headers {
		headers[header.Key] = string(header.Value)
	}

	value, err := json.DeserializeToMap(string(msg.Value))
	if err != nil {
		return domain.Event{}, fmt.Errorf("failed to deserialize message value: %w", err)
	}

	metadata := domain.EventMetadata{
		Topic:     *msg.TopicPartition.Topic,
		Partition: msg.TopicPartition.Partition,
		Offset:    int64(msg.TopicPartition.Offset),
		Timestamp: msg.Timestamp.UnixMilli(),
		KeySize:   len(msg.Key),
		ValueSize: len(msg.Value),
		Headers:   headers,
		RequestID: headers["requestId"],
	}

	event := domain.Event{
		ID:       headers["id"],
		Key:      string(msg.Key),
		Value:    value,
		Metadata: metadata,
	}

	return event, nil
}
