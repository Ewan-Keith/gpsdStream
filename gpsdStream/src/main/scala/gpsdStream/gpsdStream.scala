package gpsdStream

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.stream._
import akka.stream.alpakka.udp.Datagram
import akka.stream.alpakka.udp.scaladsl.Udp
import akka.stream.scaladsl.{Flow, Source, Tcp}
import akka.util.ByteString
import io.circe.Json
import io.circe.parser.parse
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer, StringSerializer}

import scala.concurrent.Future


object gpsdStream extends App  {

  // initialise Actor system and materialiser
  implicit val system: ActorSystem = ActorSystem("gpsdStream")
  implicit val materializer: Materializer = ActorMaterializer()

  // define the UDP location that GPSD is listening on
  val destination = new InetSocketAddress("0.0.0.0", 5001)

  // set up Kafka Consumer and Producer settings
  val consumerConfig = system.settings.config.getConfig("akka.kafka.consumer")
  val consumerSettings = ConsumerSettings(consumerConfig, new ByteArrayDeserializer, new StringDeserializer)
    .withBootstrapServers("0.0.0.0:9092")
    .withGroupId("gpsdStream")
    .withProperty(ConsumerConfig.CLIENT_ID_CONFIG, "your_client_id")

  val producerConfig = system.settings.config.getConfig("akka.kafka.producer")
  val producerSettings =
    ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)
      .withBootstrapServers("0.0.0.0:9092")

  // consume from topic "test", add a linebreak, convert to ByteSting Datagram and push to destination
  Consumer.committableSource(consumerSettings, Subscriptions.topics("test"))
    .map(msg => ByteString(msg.record.value + "\n"))
    .map(Datagram(_, destination))
    .runWith(Udp.sendSink())


  // define the TCP connection at which to connect to gpsd
  val jsonSource: Flow[ByteString, ByteString, Future[Tcp.OutgoingConnection]] =
    Tcp().outgoingConnection("0.0.0.0", 2948)

  // prepare the gpsd client message to send to gpsd once connected
  val initWatcherMsg = Source.single(ByteString("""?WATCH={"enable":true,"json":true}"""))

  // define the status values to drop from the gpsd returns
  val excludeStatuses = List("VERSION", "DEVICES", "DEVICE", "WATCH")

  // define function to extract the status value from a gpsd return
  def extractMessageClass(json: Json) = json.hcursor.downField("class").as[String]

  // return false if status is in the list to exlude, else true
  def statusDrop(exclude: List[String])(json: Json): Boolean = {
    val extractedStatus = extractMessageClass(json)

    !exclude.contains(extractedStatus.right.getOrElse("VERSION"))
  }

  // push initialisation message to gpsd tcp, then map incoming message to Kafka records and push to producer
  Source.maybe[ByteString].merge(initWatcherMsg)
    .via(jsonSource)
    .map(x => (x, parse(x.utf8String)))
    .filter(_._2.isRight)
    .filter(x => statusDrop(excludeStatuses)(x._2.right.get))
    .map(x => new ProducerRecord[String, String]("output", x._1.utf8String))
    .runWith(Producer.plainSink(producerSettings))

}
