package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/schema/field"
)

// ModelListing holds the schema definition for the ModelListing entity.
type ModelListing struct {
	ent.Schema
}

// Fields of the ModelListing.
func (ModelListing) Fields() []ent.Field {
	return []ent.Field{
		field.String("model_code").NotEmpty().MaxLen(20),
		field.String("provider").NotEmpty(),
		field.String("user_readable_name").NotEmpty().MaxLen(20),
		field.Bool("is_supported").Default(false),
	}
}

// Edges of the ModelListing.
func (ModelListing) Edges() []ent.Edge {
	return nil
}
