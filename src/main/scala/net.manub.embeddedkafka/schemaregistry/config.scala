package net.manub.embeddedkafka.schemaregistry

import java.nio.file.{Files, Path, Paths}
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

import io.confluent.kafka.schemaregistry.avro.AvroCompatibilityLevel
import net.manub.embeddedkafka.{
  EmbeddedKafkaConfig => OriginalEmbeddedKafkaConfig
}
import org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.common.config.SslConfigs._

trait EmbeddedKafkaConfig extends OriginalEmbeddedKafkaConfig {
  def kafkaSSLPort: Int
  def schemaRegistryPort: Int
  def avroCompatibilityLevel: AvroCompatibilityLevel
}

case class EmbeddedKafkaConfigImpl(
    kafkaPort: Int,
    kafkaSSLPort: Int,
    zooKeeperPort: Int,
    schemaRegistryPort: Int,
    avroCompatibilityLevel: AvroCompatibilityLevel,
    brokerProperties: Map[String, String],
    producerProperties: Map[String, String],
    consumerProperties: Map[String, String]
) extends EmbeddedKafkaConfig {
  override def numberOfThreads: Int = 3
  override def customBrokerProperties: Map[String, String] =
    brokerSSLProperties ++ brokerProperties
  override def customProducerProperties: Map[String, String] =
    clientSSLProperties ++ producerProperties
  override def customConsumerProperties: Map[String, String] =
    clientSSLProperties ++ consumerProperties

  private def brokerSSLProperties: Map[String, String] = {
    val listener = s"SSL://:$kafkaSSLPort,PLAINTEXT://:$kafkaPort"

    Map(
      SSL_KEY_PASSWORD_CONFIG -> "keypass",
      SSL_KEYSTORE_LOCATION_CONFIG -> getPath("ssl/broker/keystore.jks").toString,
      SSL_KEYSTORE_PASSWORD_CONFIG -> "keystorepassword",
      SSL_TRUSTSTORE_LOCATION_CONFIG -> getPath("ssl/broker/truststore.jks").toString,
      SSL_TRUSTSTORE_PASSWORD_CONFIG -> "truststorepassword",
      "listeners" -> listener,
      "advertised.listeners" -> listener
    )
  }

  private def clientSSLProperties: Map[String, String] = {
    Map(
      BOOTSTRAP_SERVERS_CONFIG -> s"localhost:$kafkaSSLPort",
      // Kafka 2.0 changed the default value to "https". Setting back to "" otherwise all certs must be regenerated
      SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG -> "",
      SECURITY_PROTOCOL_CONFIG -> "SSL",
      SSL_KEY_PASSWORD_CONFIG -> "keypass",
      SSL_KEYSTORE_LOCATION_CONFIG -> getPath("ssl/client/keystore.jks").toString,
      SSL_KEYSTORE_PASSWORD_CONFIG -> "keystorepassword",
      SSL_TRUSTSTORE_LOCATION_CONFIG -> getPath("ssl/client/truststore.jks").toString,
      SSL_TRUSTSTORE_PASSWORD_CONFIG -> "truststorepassword"
    )
  }

  private def getPath(resource: String): Path = {
    val in = getClass.getClassLoader.getResourceAsStream(resource)
    try {
      val target =
        Files.createTempFile("", Paths.get(resource).getFileName.toString)
      Files.copy(in, target, REPLACE_EXISTING)
      target.toFile.deleteOnExit()
      target
    } finally {
      in.close()
    }
  }
}

object EmbeddedKafkaConfig {
  implicit val defaultConfig: EmbeddedKafkaConfig = apply()

  def apply(
      kafkaPort: Int = 6001,
      kafkaSSLPort: Int = 6003,
      zooKeeperPort: Int = 6000,
      schemaRegistryPort: Int = 6002,
      avroCompatibilityLevel: AvroCompatibilityLevel =
        AvroCompatibilityLevel.NONE,
      customBrokerProperties: Map[String, String] = Map.empty,
      customProducerProperties: Map[String, String] = Map.empty,
      customConsumerProperties: Map[String, String] = Map.empty
  ): EmbeddedKafkaConfig =
    EmbeddedKafkaConfigImpl(
      kafkaPort,
      kafkaSSLPort,
      zooKeeperPort,
      schemaRegistryPort,
      avroCompatibilityLevel,
      customBrokerProperties,
      customProducerProperties,
      customConsumerProperties
    )
}
