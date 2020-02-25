package com.knoldus

  import java.io.File

  import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
  import akka.routing.RoundRobinPool
  import akka.util.Timeout
  import com.typesafe.config.ConfigFactory

  import scala.concurrent.duration._
  import scala.io.Source
  import com.knoldus.FileBasicOperation

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
      log.info(self.path + result.toString())
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
  upadateFileList.map(ll=>ref ! ll)
  //implicit val timeout: Timeout = Timeout(4.seconds)


 // system.terminate()
}


