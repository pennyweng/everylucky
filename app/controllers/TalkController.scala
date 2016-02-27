package controllers

import play.api.mvc._
import play.api.Play.current
import actors._
import actors.MyWebSocketActor._


import play.api.mvc.WebSocket.FrameFormatter

object TalkController extends Controller {
	def socket(uid : String) = WebSocket.acceptWithActor[String, String] { request => out =>
	  	MyWebSocketActor.props(uid, out)
	}


	def getUserCount(rid : String) = Action {
		Ok(getTkByRoomId(rid).size + "")
	}


}