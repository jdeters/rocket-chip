package freechips.rocketchip.rocket

import chisel3._
import scala.collection.mutable.LinkedHashMap

class CSreg(addr: Int, reg: Data)(wireUp: Data => Unit)(implicit csr: CSRFile){

  val address = addr
  val register = reg

  //we have to add this to the read_mapping so we're not seen as illigal
  csr.read_mapping += addr -> reg.asInstanceOf[Bits]

  //this is the same thing as decoded_addr()
  when(csr.io.rw.addr === address.asUInt) {
    wireUp(register)
  }

  def apply() = reg
}
