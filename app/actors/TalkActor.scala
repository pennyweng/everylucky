package actors

import akka.actor._
import scala.collection.mutable.Map
import scala.collection.mutable.MutableList

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{read, write}
import java.util.PriorityQueue

case class Msg(tp : String, body : Option[String])
case class ServerMsg(tp : String, ts : Long, body : Option[String] = None)

object MyWebSocketActor {
	val TP_CLIENT_WANT_TALK = "TCWT"
	val TP_CLIENT_SEND_MSG = "TCSM"
	val TP_CLIENT_SEND_END = "TCSE"
	val TP_CLIENT_CANCEL_WAIT = "TCCW"
	val TP_CLIENT_GET_COUNT = "TCGC"

	val TP_SERVER_START_TALK = "TSST"
	val TP_SERVER_SEND_MSG = "TSSM"	
	val TP_SERVER_END_TALK = "TSET"
	val TP_SERVER_SEND_COUNT = "TSSC"

	val connected = Map.empty[String, ActorRef]
	val room = Map.empty[String, String]

	var talking = Map.empty[String, String]
	var talking1 = Map.empty[String, String]
	var talking2 = Map.empty[String, String]
	var talking3 = Map.empty[String, String]
	var talking4 = Map.empty[String, String]

	val waitQueue = new PriorityQueue[String]()
	val waitQueue1 = new PriorityQueue[String]()
	val waitQueue2 = new PriorityQueue[String]()
	val waitQueue3 = new PriorityQueue[String]()
	val waitQueue4 = new PriorityQueue[String]()

	// user info
	val userInfoMap = Map.empty[String, String]

  	def props(uid : String, out: ActorRef) = {
  		connected += uid -> out
  		Props(new MyWebSocketActor(uid, out))
  	}

	def getTkByRoomId(rid : String) = {
		rid match {
			case "0" => talking
			case "1" => talking1
			case "2" => talking2
			case "3" => talking3
			case "4" => talking4
			case _ => talking
		}
	}  	
}

class MyWebSocketActor(uid : String, out: ActorRef) extends Actor {
	import MyWebSocketActor._
	// implicit val formats = DefaultFormats
	implicit val formats = Serialization.formats(NoTypeHints)

	def receive = {
		case raw : String =>
			println(s"receive msg ${raw}")
			try {
				val msg = read[Msg](raw)//parse(raw).extract[Msg]
				
				msg match {
					case Msg(TP_CLIENT_WANT_TALK, _) =>
						println(s"TP_CLIENT_WANT_TALK ${msg.body}")
						val (wq, tk) = msg.body.getOrElse("0") match {
								case "0" => (waitQueue, talking)
								case "1" => (waitQueue1, talking1)
								case "2" => (waitQueue2, talking2)
								case "3" => (waitQueue3, talking3)
								case "4" => (waitQueue4, talking4)
								case _ =>   (waitQueue, talking)			
							} 

						room += uid -> msg.body.getOrElse("0")
						if(wq.size > 0) {
							val buid = wq.poll

							if(connected.contains(uid) && connected.contains(buid)) {
								tk += buid -> uid
								tk += uid -> buid
								
								val resp = write(ServerMsg(TP_SERVER_START_TALK, System.currentTimeMillis))
								connected(uid) ! resp
								connected(buid) ! resp	
				  			} else {
				  				println(s"user1 connection status is :${connected.contains(uid)}, user2 connection status is :${connected.contains(buid)}")
								wq.offer(uid)				  				
				  			}
						} else {
							println(s"queue is empty, put user ${uid} into the queue")
							wq.offer(uid)
						}
					case Msg(TP_CLIENT_SEND_MSG, _) =>
						val tk = getTk(uid)

						if(tk.contains(uid)) {
							val buid = tk(uid) 
							if(connected.contains(buid)) {
								connected(buid) ! write(ServerMsg(TP_SERVER_SEND_MSG, System.currentTimeMillis, msg.body))
							} // else connected(uid) ! write(ServerMsg(TP_SERVER_END_TALK, System.currentTimeMillis))
						} else connected(uid) ! write(ServerMsg(TP_SERVER_END_TALK, System.currentTimeMillis))
					
					case Msg(TP_CLIENT_SEND_END, _) =>
						println(s"TP_CLIENT_SEND_END ${msg.body}")
						val tk = getTk(uid)

						if(tk.contains(uid)) {
							val buid = tk(uid) 

							if(connected.contains(buid)) {
								val resp = write(ServerMsg(TP_SERVER_END_TALK, System.currentTimeMillis))
								connected(buid) ! resp
							}

							tk.remove(uid)
							tk.remove(buid)
						}

						room.remove(uid)

					case Msg(TP_CLIENT_CANCEL_WAIT, _) =>
						val wq = getWQ(uid)
						if(wq.size > 0) {
							wq.poll
							println(s"wq size ${wq.size}")
						}
						room.remove(uid)

					// case Msg(TP_CLIENT_GET_COUNT, _) => 
					// 	val count = getTkByRoomId(msg.body.getOrElse("0")).size

					// 	connected(uid) ! write(ServerMsg(TP_SERVER_SEND_COUNT, 
					// 		System.currentTimeMillis, Some(count.toString)))

					case _ => println("receive error msg")
				}
			} catch {
				case _ => println("receive error msg")
			}
	  	case _ => println("receive error msg")	
	}

	def getWQ(uid : String) = {
		room.getOrElse(uid, "0") match {
			case "0" => waitQueue
			case "1" => waitQueue1
			case "2" => waitQueue2
			case "3" => waitQueue3
			case "4" => waitQueue4
			case _ => waitQueue
		}
	}



	def getTk(uid : String) = {
		room.getOrElse(uid, "0") match {
			case "0" => talking
			case "1" => talking1
			case "2" => talking2
			case "3" => talking3
			case "4" => talking4
			case _ => talking
		}
	}

	override def postStop() = {
		// if(talking.contains(uid)) {
		// 	val buid = talking(uid) 

		// 	if(connected.contains(buid)) {
		// 		connected(buid) ! write(ServerMsg(TP_SERVER_END_TALK, System.currentTimeMillis))
		// 	} 
			
		// 	talking.remove(uid)
		// 	talking.remove(buid)
		// }

		connected.remove(uid)
	}
}