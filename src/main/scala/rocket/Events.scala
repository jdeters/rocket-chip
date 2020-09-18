// See LICENSE.Berkeley for license details.
// See LICENSE.SiFive for license details.

package freechips.rocketchip.rocket

import chisel3._
import freechips.rocketchip.util._
import freechips.rocketchip.util.property._
import scala.collection.mutable._

/** A perfromance monitor event
  */
case class Event(name: String, id: UInt, module: EventModule)

class EventModule extends Module {
  val io = IO(new Bundle {
    val conditionIn = Input(Bool())
    val conditionOut = Output(Bool())
  })

  io.conditionOut := io.conditionIn
}

object EventFactory {
  var events = ArrayBuffer[Event]()

  def apply(name: String, condition: () => Bool, id: UInt) {
    val newCondtionModule = Module(new EventModule)

    newCondtionModule.io.conditionIn := condition()

    val newEvent = Event(name, id, newCondtionModule)

    events += newEvent
  }

  def printEventNames = {
    println(s"There are ${events.size} events")
    events.foreach(event => println(s"Event: ${event.name}"))
  }

  def connectEvents (csr: CSRFile) = {
    for((event, i) <- events zipWithIndex)  {
      when((event.module).io.conditionOut) {
        csr.io.incomingEvents(i) := event.id
      }
    }
  }

}
