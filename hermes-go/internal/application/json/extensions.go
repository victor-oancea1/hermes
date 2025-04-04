package json

import (
	"encoding/json"
)

func DeserializeToMap(jsonStr string) (map[string]interface{}, error) {
	var result map[string]interface{}
	err := json.Unmarshal([]byte(jsonStr), &result)
	return result, err
}

func SerializeFromMap(data map[string]interface{}) (string, error) {
	bytes, err := json.Marshal(data)
	return string(bytes), err
}
