// See LICENSE.Berkeley for license details.
// See LICENSE.SiFive for license details.

package freechips.rocketchip.rocket

import chisel3._
import freechips.rocketchip.util._
import freechips.rocketchip.util.property._
import scala.collection.mutable._

/** A perfromance monitor event
  */
case class Event(name: String, id: UInt, handle: Data)

object EventFactory {
  val events = ArrayBuffer[Event]()

  def apply(name: String, condition: () => Bool, id: UInt) {
    events += Event(name, id, SignalThreadder.pluck(name, condition()))
  }

  def printEventNames = {
    println(s"There are ${events.size} events")
    events.foreach(event => println(s"Event: ${event.name}"))
  }

  def connectEvents (csr: CSRFile) = {
    for((event, i) <- events zipWithIndex)  {
      when(event.handle.asInstanceOf[Bool]) {
        csr.io.incomingEvents(i) := event.id
      }
    }
  }

}
