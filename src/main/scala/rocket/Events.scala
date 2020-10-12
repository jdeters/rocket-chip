// See LICENSE.Berkeley for license details.
// See LICENSE.SiFive for license details.

package freechips.rocketchip.rocket

import chisel3._
import freechips.rocketchip.util._
import scala.collection.mutable._

trait EventIO {
  val incomingEvents = Input(Vec(40, UInt(32.W)))
}

case class Event(name: String, id: UInt, handle: Data)

object EventFactory {
  val events = ArrayBuffer[Event]()

  def apply(name: String, condition: () => Bool, id: UInt) {
    //NOTE: if the signals are plucked merged into another pluck, this could possbily lead to a lot of reduency
    println(s"Adding event $name")
    events += Event(name, id, SignalThreadder.pluck(name, condition()))
  }

  def printEventNames = {
    println(s"There are ${events.size} events")
    events.foreach(event => println(s"Event: ${event.name}"))
  }

  //given a CSR file, connect the events WITHIN that file
  def connectIO (csr: CSRFile) = {
    //create the logic to check incoming event signals
    for(incomingEvent <- csr.io.incomingEvents){
      for((configuredEvent, register) <- csr.reg_hpmevent zip csr.reg_hpmcounter) {
        when(configuredEvent === incomingEvent) {
          //because this is not IO, this function must be called inside the CSRFile module
          register := register + 1.U
        }
      }
    }
  }

  //this can be called from anywhere as long as the IO is present in the CSR file
  def connectEvents (csr: CSRFile) = {
    for((event, i) <- events zipWithIndex)  {
      when(event.handle.asInstanceOf[Bool]) {
        csr.io.incomingEvents(i) := event.id
      }
    }
  }

}
