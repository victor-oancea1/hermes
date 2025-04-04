package handler

import (
	"context"
	"net/http"
	"strconv"
	"strings"

	"github.com/gin-gonic/gin"
	"github.com/victor-oancea1/hermes/hermes-go/internal/application"
	"github.com/victor-oancea1/hermes/hermes-go/internal/domain"
)

type MessageHandler struct {
	eventReceiver application.EventReceiver
	eventSender   application.EventSender
}

func NewMessageHandler(eventReceiver application.EventReceiver, eventSender application.EventSender) *MessageHandler {
	return &MessageHandler{
		eventReceiver: eventReceiver,
		eventSender:   eventSender,
	}
}

func (h *MessageHandler) GetMessages(c *gin.Context) {
	topic := c.Query("topic")
	if topic == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "topic is required"})
		return
	}

	expression := c.DefaultQuery("expression", "*")
	
	limitStr := c.DefaultQuery("limit", "10")
	limit, err := strconv.Atoi(limitStr)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid limit"})
		return
	}

	offsetType := c.DefaultQuery("offsetType", string(domain.OffsetTypeStart))
	var offset domain.Offset
	
	switch domain.OffsetType(strings.ToUpper(offsetType)) {
	case domain.OffsetTypeStart:
		offset = domain.Offset{OffsetType: domain.OffsetTypeStart}
	case domain.OffsetTypeEnd:
		offset = domain.Offset{OffsetType: domain.OffsetTypeEnd}
	case domain.OffsetTypeSpecific:
		offsetValueStr := c.Query("offsetValue")
		if offsetValueStr == "" {
			c.JSON(http.StatusBadRequest, gin.H{"error": "offsetValue is required for SPECIFIC offset type"})
			return
		}
		offsetValue, err := strconv.Atoi(offsetValueStr)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": "invalid offsetValue"})
			return
		}
		offset = domain.Offset{
			OffsetType: domain.OffsetTypeSpecific,
			Value:      &offsetValue,
		}
	default:
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid offsetType"})
		return
	}

	receiverOption := domain.ReceiverOption{
		Offset:     offset,
		Topic:      topic,
		Expression: expression,
		Limit:      limit,
	}

	ctx, cancel := context.WithCancel(c.Request.Context())
	defer cancel()
	
	eventChan, err := h.eventReceiver.Receive(ctx, []domain.ReceiverOption{receiverOption})
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	events := []domain.Event{}
	for event := range eventChan {
		events = append(events, event)
	}

	c.JSON(http.StatusOK, events)
}

func (h *MessageHandler) SendMessages(c *gin.Context) {
	var events []domain.Event
	if err := c.ShouldBindJSON(&events); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	for i, event := range events {
		if event.Key == "" {
			c.JSON(http.StatusBadRequest, gin.H{"error": "event key is required", "index": i})
			return
		}
		if event.Metadata.Topic == "" {
			c.JSON(http.StatusBadRequest, gin.H{"error": "event topic is required", "index": i})
			return
		}
	}

	ctx, cancel := context.WithCancel(c.Request.Context())
	defer cancel()
	
	eventChan, err := h.eventSender.Send(ctx, events)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	sentEvents := []domain.Event{}
	for event := range eventChan {
		sentEvents = append(sentEvents, event)
	}

	c.JSON(http.StatusOK, sentEvents)
}
