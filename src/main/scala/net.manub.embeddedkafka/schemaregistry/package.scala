package net.manub.embeddedkafka

import io.confluent.kafka.streams.serdes.avro.{
  GenericAvroSerde,
  SpecificAvroSerde
}
import net.manub.embeddedkafka.schemaregistry.EmbeddedKafka.{
  configForSchemaRegistry,
  consumerConfigForSchemaRegistry
}
import net.manub.embeddedkafka.schemaregistry.{
  EmbeddedKafkaConfig => EmbeddedKafkaSRConfig
}
import org.apache.avro.generic.GenericRecord
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.{Serde, Serializer, Deserializer}

import scala.collection.JavaConverters._

package object schemaregistry {
  implicit def specificAvroSerde[T <: SpecificRecord](
      implicit config: EmbeddedKafkaSRConfig): Serde[T] = {
    val serde = new SpecificAvroSerde
    serde.configure(consumerConfigForSchemaRegistry.asJava, false)
    serde.asInstanceOf[Serde[T]]
  }

  implicit def specificAvroSerializer[T <: SpecificRecord](
      implicit config: EmbeddedKafkaSRConfig): Serializer[T] = {
    specificAvroSerde.serializer.asInstanceOf[Serializer[T]]
  }

  implicit def specificAvroDeserializer[T <: SpecificRecord](
      implicit config: EmbeddedKafkaSRConfig): Deserializer[T] = {
    specificAvroSerde.deserializer.asInstanceOf[Deserializer[T]]
  }

  implicit def genericAvroSerde(
      implicit config: EmbeddedKafkaSRConfig): Serde[GenericRecord] = {
    val serde = new GenericAvroSerde
    serde.configure(configForSchemaRegistry.asJava, false)
    serde
  }

  implicit def genericAvroSerializer(
      implicit config: EmbeddedKafkaSRConfig): Serializer[GenericRecord] = {
    genericAvroSerde.serializer
  }

  implicit def genericAvroDeserializer(
      implicit config: EmbeddedKafkaSRConfig): Deserializer[GenericRecord] = {
    genericAvroSerde.deserializer
  }
}
