package rht.hack

import akka.{Done}
import akka.actor.{Actor}
import akka.event.Logging
import akka.stream.{CompletionStrategy, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.redis.RedisClient
import rht.common.domain.candles.Candle
import scala.concurrent.{Future}

class TestActor extends Actor {
  val log = Logging(context.system, this)
  implicit val system = context.system

  def filter(figi: String) = {
    Flow[Candle].filter(x => x.figi.value == figi)
  }

  def sinkCreate: ((Double, Candle) => Double) => Sink[Candle, Future[Double]] = {
    Sink.fold[Double, Candle](0)
  }

  def sinkOperation(sink: ((Double, Candle) => Double) => Sink[Candle, Future[Double]], oper: String) ={
    oper match {
      case "max" =>
        sink((value, candle) => math.max(value, candle.details.high.doubleValue()))
      case "min" =>
        sink((value, candle) => math.min(value, candle.details.high.doubleValue()))
      case "avg" =>
        sink((value, candle) => math.max(value, candle.details.high.doubleValue()))
    }
  }

  def senderRedis(candle: Candle): Unit ={
    val redis = new RedisClient("redis-19348.c263.us-east-1-2.ec2.cloud.redislabs.com", port = 19348, secret = Some("NvRJ6X9FGgXOC5KQ8MTcbKO3AdTYnD5J"))
    val json = candle.details.close.toDouble.toString
    candle.figi.value match {
      case "BTC" => redis.set("BTC", json)
      case "ETH" => redis.set("ETH", json)
      case "BNB" => redis.set("BNB", json)
      case "DOGE" => redis.set("DOGE", json)
      case "DOT" => redis.set("DOT", json)
      case "ADA" => redis.set("ADA", json)
    }
    redis.close()
  }

  def writerRedis(key: String, value: String): Unit ={
    val redis = new RedisClient("redis-19348.c263.us-east-1-2.ec2.cloud.redislabs.com", port = 19348, secret = Some("NvRJ6X9FGgXOC5KQ8MTcbKO3AdTYnD5J"))
    redis.set(key, value)
    redis.close()
  }

  def readerRedis(key: String) = {
    val redis = new RedisClient("redis-19348.c263.us-east-1-2.ec2.cloud.redislabs.com", port = 19348, secret = Some("NvRJ6X9FGgXOC5KQ8MTcbKO3AdTYnD5J"))
    val json = redis.get(key)
    redis.close()
    json.get
  }

  override def receive: Receive = {
    case candle: Candle => {
      val cm: PartialFunction[Any, CompletionStrategy] = {
        case Done =>
          CompletionStrategy.immediately
      }


      val close = readerRedis("BTC").toDouble


      val percent = 5

      println("now" + candle.details.close.doubleValue())
      println("close" + (close * (100+percent))/100)

      if (candle.details.close.doubleValue() >= (close * (100+percent))/100){
        println("change")
      }


      val ref = Source
        .actorRef[Candle](
          completionMatcher = cm,
          failureMatcher = PartialFunction.empty[Any, Throwable],
          bufferSize = 2000,
          overflowStrategy = OverflowStrategy.fail)
        .toMat(Sink.foreach(senderRedis))(Keep.left)
        .run()

      ref ! candle
    }
    case _ => println("received unknown message")

  }
}