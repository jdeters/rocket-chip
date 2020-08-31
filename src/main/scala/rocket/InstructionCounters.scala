package freechips.rocketchip.rocket

import freechips.rocketchip.config.Parameters
import freechips.rocketchip.tile._
import chisel3._
import chisel3.util._
import Instructions._

class InstructionCountersIO (implicit p: Parameters) extends CoreBundle {
  val inst = Input(UInt(32.W))
  val valid = Input(Bool())
}

class InstructionCounters(decode_table: Seq[(BitPat, List[BitPat])]) (implicit p: Parameters) extends CoreModule {

  val io = IO(new InstructionCountersIO)

  //create a new set of resetable registers that always go back to 0
  val counters = RegInit(VecInit(Seq.fill(decode_table.size+1)(0.U(32.W))))

  //construct a new list that maps the instructions to a counter
  val instMap = for (i <- 0 until decode_table.size) yield {
    //doing this to avoid using util.Lookup which is likely to be deprecated
    decode_table(i)._1 -> i.U(32.W);
  }

  //look up the register and increase the count
  val insCounter = counters(Lookup(io.inst, counters(decode_table.size), instMap))

  when(io.valid) {
    insCounter := insCounter + 1.U
    printf("Executing instruction: %x has been seen %d times\n", io.inst, insCounter);
  }
}
