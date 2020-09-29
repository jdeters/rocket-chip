package freechips.rocketchip.system

import chiselaspects._
import scala.meta._
import scala.meta.contrib._

class EventAspect (tree: Tree) extends Aspect(tree) {
  //adding events to the Data Cache
  around(init"DCacheModuleImpl", q"""
    EventFactory("DCache miss", () => edge.done(tl_out_a), 0x13)
    EventFactory("DCache release", () => edge.done(tl_out_c), 0x14)
    EventFactory("DTLB miss", () => io.ptw.req.fire(), 0x16)
  """)

  //adding events to the Non-Blocking Cache
  around(init"NonBlockingDCacheModule(this)", q"""
    EventFactory("DCache miss", () => edge.done(tl_out.a), 0x13)
    EventFactory("DCache release", () => edge.done(tl_out.c), 0x14)
    EventFactory("DTLB miss", () => io.ptw.req.fire(), 0x16)
  """)

  //adding event to the Instruction Cache
  around(init"ICacheModule(this)", q"""
    EventFactory("ICache miss", () => refill_fire, 0x12)
    EventFactory("ICache blocked", () => !(io.resp.valid || RegNext(io.resp.valid)), 0xc)
  """)

  //adding events to the L2 cache
  //things are weird with single line quasiquotes
  around(init"PTW(outer.nPTWPorts)(outer.dcache.node.edges.out(0), outer.p)", q"{EventFactory(${"L2 TLB miss"}, () => l2Miss, 0x17)}")

  //adding events to the Core and connecting them to the CSR
  around(init"RocketImpl", q"""
    EventFactory("exception", () => false.B, 0x0.U)
    EventFactory("load", () => id_ctrl.mem && id_ctrl.mem_cmd === M_XRD && !id_ctrl.fp, 0x1)
    EventFactory("store", () => id_ctrl.mem && id_ctrl.mem_cmd === M_XWR && !id_ctrl.fp, 0x2)
    EventFactory("amo", () => Bool(usingAtomics) && id_ctrl.mem && (isAMO(id_ctrl.mem_cmd) || id_ctrl.mem_cmd.isOneOf(M_XLR, M_XSC)), 0x3)
    EventFactory("system", () => id_ctrl.csr =/= CSR.N, 0x4)
    EventFactory("arith", () => id_ctrl.wxd && !(id_ctrl.jal || id_ctrl.jalr || id_ctrl.mem || id_ctrl.fp || id_ctrl.mul || id_ctrl.div || id_ctrl.csr =/= CSR.N), 0x5)
    EventFactory("branch", () => id_ctrl.branch, 0x6)
    EventFactory("jal", () => id_ctrl.jal, 0x7)
    EventFactory("jalr", () => id_ctrl.jalr, 0x8)
    EventFactory("load-use interlock", () => id_ex_hazard && ex_ctrl.mem || id_mem_hazard && mem_ctrl.mem || id_wb_hazard && wb_ctrl.mem, 0x9)
    EventFactory("long-latency interlock", () => id_sboard_hazard, 0xa)
    EventFactory("csr interlock", () => id_ex_hazard && ex_ctrl.csr =/= CSR.N || id_mem_hazard && mem_ctrl.csr =/= CSR.N || id_wb_hazard && wb_ctrl.csr =/= CSR.N, 0xb)
    EventFactory("DCache blocked", () => id_ctrl.mem && dcache_blocked, 0xd)
    EventFactory("branch misprediction", () => take_pc_mem && mem_direction_misprediction, 0xe)
    EventFactory("control-flow target misprediction", () => take_pc_mem && mem_misprediction && mem_cfi && !mem_direction_misprediction && !(io.imem.resp.valid || RegNext(io.imem.resp.valid)), 0xf)
    EventFactory("flush", () => wb_reg_flush_pipe, 0x10)
    EventFactory("replay", () => replay_wb, 0x11)
    if (usingMulDiv) {
      EventFactory("mul", () => if (pipelinedMul) id_ctrl.mul else id_ctrl.div && (id_ctrl.alu_fn & ALU.FN_DIV) =/= ALU.FN_DIV, 0x18)
      EventFactory("div", () => if (pipelinedMul) id_ctrl.div else id_ctrl.div && (id_ctrl.alu_fn & ALU.FN_DIV) === ALU.FN_DIV, 0x19)
      EventFactory("mul/div interlock", () => id_ex_hazard && (ex_ctrl.mul || ex_ctrl.div) || id_mem_hazard && (mem_ctrl.mul || mem_ctrl.div) || id_wb_hazard && wb_ctrl.div, 0x1a)
    }
    if (usingFPU) {
      EventFactory("fp load", () => id_ctrl.fp && io.fpu.dec.ldst && io.fpu.dec.wen, 0x1b)
      EventFactory("fp store", () => id_ctrl.fp && io.fpu.dec.ldst && !io.fpu.dec.wen, 0x1c)
      EventFactory("fp add", () => id_ctrl.fp && io.fpu.dec.fma && io.fpu.dec.swap23, 0x1d)
      EventFactory("fp mul", () => id_ctrl.fp && io.fpu.dec.fma && !io.fpu.dec.swap23 && !io.fpu.dec.ren3, 0x1e)
      EventFactory("fp mul-add", () => id_ctrl.fp && io.fpu.dec.fma && io.fpu.dec.ren3, 0x1f)
      EventFactory("fp div/sqrt", () => id_ctrl.fp && (io.fpu.dec.div || io.fpu.dec.sqrt), 0x20)
      EventFactory("fp other", () => id_ctrl.fp && !(io.fpu.dec.ldst || io.fpu.dec.fma || io.fpu.dec.div || io.fpu.dec.sqrt), 0x21)
      EventFactory("fp interlock", () => id_ex_hazard && ex_ctrl.fp || id_mem_hazard && mem_ctrl.fp || id_wb_hazard && wb_ctrl.fp || id_ctrl.fp && id_stall_fpu, 0x22)
    }

    EventFactory.connectEvents(csr)
  """)

  //creating the IO for events  in the CSR
  around(init"CSRFile(customCSRs = coreParams.customCSRs.decls)", q"{EventFactory.connectIO(this)}")

}
