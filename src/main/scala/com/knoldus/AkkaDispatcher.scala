package com.knoldus

  import java.io.File

  import akka.pattern.ask
  import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
  import akka.routing.RoundRobinPool
  import akka.util.Timeout
  import com.typesafe.config.ConfigFactory

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._
  import scala.io.Source

  import scala.concurrent.Future

class AkkaDispatcher  extends  Actor with ActorLogging {
  def readAndOperate(filename: File): Map[String, Option[Int]] = {
    val copyFileName = filename.toString
    val source = Source.fromFile(s"$copyFileName")
    val resultMap = source.getLines().flatMap(_.split(" ")).toList.groupBy((word: String) => word).mapValues(_.length)
    source.close()
    Map("error" -> resultMap.get("[ERROR]")) ++ Map("warn" -> resultMap.get("[WARN]")) ++ Map("info" -> resultMap.get("[INFO]"))
  }

 override def receive: Receive = {
    case msg: File =>
      val result = readAndOperate(msg)
      //log.info(self.path + result.toString())
        println(self.path+result.toString)
        sender() ! result
    case _ => log.info("default case")
  }
}

object AkkaDispatcher extends App  {
  val config = ConfigFactory.load()
  val system = ActorSystem("LogFilesActorSystem", config.getConfig("configuration"))
  val confStr="fixed-dispatcher"
  val threads=5
  val ref = system.actorOf(RoundRobinPool(threads).props(Props[AkkaDispatcher]), "FileOperation")
  val pathObj = new File("/home/knoldus/Downloads/Io2/")
  val list = pathObj.listFiles().toList
  val upadateFileList = list.filter(_.isFile)
  implicit val timeout: Timeout = Timeout(8.seconds)
  val result = upadateFileList.map(ll=>ref ? ll)

  val newResult=Future.sequence(result)
  val finalResult = newResult.mapTo[List[Map[String, Option[Int]]]].map(_.foldLeft(Map("error"->0, "warn"->0, "info"->0)){ (result, countMap) => {
    result++Map("error"->(result("error")+ countMap("error").getOrElse(0)))++Map("warn"->(result("warn")+ countMap("warn").getOrElse(0)))++Map("info"->(result("info")+ countMap("info").getOrElse(0)))
  }})

  finalResult.map(println)


  //system.terminate()
}


