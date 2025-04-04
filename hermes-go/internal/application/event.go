package application

import (
	"context"

	"github.com/victor-oancea1/hermes/hermes-go/internal/domain"
)

type EventReceiver interface {
	Receive(ctx context.Context, receiverOptions []domain.ReceiverOption) (<-chan domain.Event, error)
}

type EventSender interface {
	Send(ctx context.Context, events []domain.Event) (<-chan domain.Event, error)
}
