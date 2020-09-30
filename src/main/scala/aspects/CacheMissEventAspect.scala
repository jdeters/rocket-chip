package freechips.rocketchip.system

import chiselaspects._
import scala.meta._
import scala.meta.contrib._

class CacheMissEventAspect (tree: Tree) extends Aspect(tree) {
  //adding events to the Data Cache
  after(init"DCacheModuleImpl", q"""
    EventFactory("DCache miss", () => edge.done(tl_out_a), 0x13)
    EventFactory("DTLB miss", () => io.ptw.req.fire(), 0x16)
  """, last = true)

  //adding events to the Non-Blocking Cache
  after(init"NonBlockingDCacheModule(this)", q"""
    EventFactory("DCache miss", () => edge.done(tl_out.a), 0x13)
    EventFactory("DTLB miss", () => io.ptw.req.fire(), 0x16)
  """, last = true)

  //adding event to the Instruction Cache
  after(init"ICacheModule(this)", q"{EventFactory(${"ICache miss"}, () => refill_fire, 0x12)}", last = true)

  //adding events to the L2 cache
  //things are weird with single line quasiquotes
  after(init"PTW(outer.nPTWPorts)(outer.dcache.node.edges.out(0), outer.p)", q"{EventFactory(${"L2 TLB miss"}, () => l2Miss, 0x17)}", last = true)

}
