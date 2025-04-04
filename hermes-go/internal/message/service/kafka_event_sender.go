package service

import (
	"context"
	"fmt"

	"github.com/confluentinc/confluent-kafka-go/v2/kafka"
	"github.com/google/uuid"
	"github.com/victor-oancea1/hermes/hermes-go/internal/application/json"
	"github.com/victor-oancea1/hermes/hermes-go/internal/domain"
)

type CorrelationMetadata struct {
	EventID   string
	RequestID string
}

type KafkaEventSender struct {
	producer *kafka.Producer
}

func NewKafkaEventSender(producer *kafka.Producer) *KafkaEventSender {
	return &KafkaEventSender{
		producer: producer,
	}
}

func (k *KafkaEventSender) Send(ctx context.Context, events []domain.Event) (<-chan domain.Event, error) {
	eventChan := make(chan domain.Event)

	if len(events) == 0 {
		close(eventChan)
		return eventChan, nil
	}

	deliveryChan := make(chan kafka.Event)
	go func() {
		defer close(eventChan)
		
		for range events {
			select {
			case <-ctx.Done():
				return
			case e := <-deliveryChan:
				if e == nil {
					continue
				}

				switch ev := e.(type) {
				case *kafka.Message:
					metadata, ok := ev.Opaque.(CorrelationMetadata)
					if !ok {
						fmt.Println("Failed to get correlation metadata")
						continue
					}

					var sentEvent domain.Event
					for _, event := range events {
						if event.ID == metadata.EventID {
							sentEvent = event
							break
						}
					}

					if ev.TopicPartition.Error != nil {
						fmt.Printf("Delivery failed: %v\n", ev.TopicPartition.Error)
						continue
					}

					eventChan <- sentEvent
				}
			}
		}
	}()

	for _, event := range events {
		valueStr, err := json.SerializeFromMap(event.Value)
		if err != nil {
			return nil, fmt.Errorf("failed to serialize event value: %w", err)
		}

		headers := []kafka.Header{}
		if event.Metadata.Headers != nil {
			for k, v := range event.Metadata.Headers {
				headers = append(headers, kafka.Header{
					Key:   k,
					Value: []byte(v),
				})
			}
		}

		eventID := event.ID
		if eventID == "" {
			eventID = uuid.New().String()
			headers = append(headers, kafka.Header{
				Key:   "id",
				Value: []byte(eventID),
			})
		}

		requestID := event.Metadata.RequestID
		if requestID != "" {
			headers = append(headers, kafka.Header{
				Key:   "requestId",
				Value: []byte(requestID),
			})
		}

		correlationMetadata := CorrelationMetadata{
			EventID:   eventID,
			RequestID: requestID,
		}

		err = k.producer.Produce(&kafka.Message{
			TopicPartition: kafka.TopicPartition{
				Topic:     &event.Metadata.Topic,
				Partition: kafka.PartitionAny,
			},
			Key:     []byte(event.Key),
			Value:   []byte(valueStr),
			Headers: headers,
			Opaque:  correlationMetadata,
		}, deliveryChan)

		if err != nil {
			return nil, fmt.Errorf("failed to produce message: %w", err)
		}
	}

	return eventChan, nil
}
