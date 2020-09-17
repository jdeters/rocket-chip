// See LICENSE.Berkeley for license details.
// See LICENSE.SiFive for license details.

package freechips.rocketchip.rocket

import Chisel._
import freechips.rocketchip.util._
import freechips.rocketchip.util.property._
import scala.collection.mutable._

/** A perfromance monitor event
  */
case class Event(name: String, condition: () => Bool, id: UInt)

object EventFactory {
  var events = ArrayBuffer[Event]()

  def apply(name: String, condition: () => Bool, id: UInt) {
    val newEvent = Event(name, condition, id)

    events += newEvent
  }

  def printEventNames = {
    println(s"There are ${events.size} events")
    events.foreach(event => println(s"Event: ${event.name}"))
  }

  def connectEvents (csr: CSRFile) = {
    for((event, i) <- events zipWithIndex)  {
      when(event.condition()) {
        csr.io.incomingEvents(i) := event.id
      }
    }
  }

}
