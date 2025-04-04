package main

import (
	"fmt"
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/confluentinc/confluent-kafka-go/v2/kafka"
	"github.com/gin-gonic/gin"
	"github.com/victor-oancea1/hermes/hermes-go/internal/domain"
	"github.com/victor-oancea1/hermes/hermes-go/internal/message/service"
	"github.com/victor-oancea1/hermes/hermes-go/internal/web/handler"
)

func main() {
	bootstrapServers := getEnv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
	
	producer, err := kafka.NewProducer(&kafka.ConfigMap{
		"bootstrap.servers": bootstrapServers,
	})
	if err != nil {
		log.Fatalf("Failed to create producer: %s", err)
	}
	defer producer.Close()

	receiverFactory := func(option domain.ReceiverOption) *kafka.Consumer {
		consumer, err := kafka.NewConsumer(&kafka.ConfigMap{
			"bootstrap.servers":  bootstrapServers,
			"group.id":           "hermes-consumer-group",
			"auto.offset.reset":  "earliest",
			"enable.auto.commit": false,
		})
		if err != nil {
			log.Printf("Failed to create consumer: %s", err)
			return nil
		}
		return consumer
	}

	kafkaEventSender := service.NewKafkaEventSender(producer)
	kafkaEventReceiver := service.NewKafkaEventReceiver(receiverFactory)

	messageHandler := handler.NewMessageHandler(kafkaEventReceiver, kafkaEventSender)

	router := gin.Default()
	
	router.GET("/messages", messageHandler.GetMessages)
	router.POST("/messages", messageHandler.SendMessages)

	go func() {
		port := getEnv("PORT", "8080")
		if err := router.Run(":" + port); err != nil {
			log.Fatalf("Failed to start server: %s", err)
		}
	}()

	fmt.Println("Hermes Go server started")

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit
	
	fmt.Println("Shutting down server...")
}

func getEnv(key, defaultValue string) string {
	value := os.Getenv(key)
	if value == "" {
		return defaultValue
	}
	return value
}
