package domain

type EventMetadata struct {
	Topic      string            `json:"topic"`
	RequestID  string            `json:"requestId,omitempty"`
	Partition  int32             `json:"partition,omitempty"`
	Offset     int64             `json:"offset,omitempty"`
	Timestamp  int64             `json:"timestamp,omitempty"`
	KeySize    int               `json:"keySize,omitempty"`
	ValueSize  int               `json:"valueSize,omitempty"`
	Headers    map[string]string `json:"headers,omitempty"`
}

type Event struct {
	ID       string                 `json:"id,omitempty"`
	Key      string                 `json:"key"`
	Value    map[string]interface{} `json:"value"`
	Metadata EventMetadata          `json:"metadata"`
}

type OffsetType string

const (
	OffsetTypeStart OffsetType = "START"
	OffsetTypeEnd OffsetType = "END"
	OffsetTypeSpecific OffsetType = "SPECIFIC"
)

type Offset struct {
	OffsetType OffsetType `json:"offsetType"`
	Value      *int       `json:"value,omitempty"`
}

type ReceiverOption struct {
	Offset    Offset  `json:"offset"`
	Topic     string  `json:"topic"`
	Expression string  `json:"expression"`
	Limit     int     `json:"limit"`
}
