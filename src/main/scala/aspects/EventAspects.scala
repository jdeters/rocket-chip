package freechips.rocketchip.system

import chiselaspects._
import scala.meta._
import scala.meta.contrib._

class EventAspect (tree: Tree) extends Aspect(tree) {

  //adding events to the Core and connecting them to the CSR
  after(init"RocketImpl", q"{EventFactory.connectEvents(csr)}")

  //creating the IO for events  in the CSR
  after(init"CSRFile(customCSRs = coreParams.customCSRs.decls)", q"{EventFactory.connectIO(this)}", last = true)

}
