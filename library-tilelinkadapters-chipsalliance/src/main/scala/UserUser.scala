// See LICENSE.SiFive for license details.

package freechips.rocketchip.tilelink

import chisel3._
import chipsalliance.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy._
import scala.reflect.ClassTag

class TLUserUser[T <: UserBits : ClassTag](meta: T, f: (TLBundleA, TLClientParameters) => UInt)(implicit p: Parameters) extends LazyModule
{
  val node = TLAdapterNode(
    clientFn  = { cp => cp.addUser(meta) },
    managerFn = { mp => mp })

  lazy val module = new LazyModuleImp(this) {
    (node.in zip node.out) foreach { case ((in, edgeIn), (out, edgeOut)) =>
      out <> in

      out.a.bits.user.foreach { u =>
        val mux = edgeOut.putUser(in.a.bits.user.getOrElse(0.U), Seq(x => f(in.a.bits, x)))
        u := mux(out.a.bits.source)
      }
    }
  }
}

object TLUserUser {
  def apply[T <: UserBits : ClassTag](meta: T, f: (TLBundleA, TLClientParameters) => UInt)(implicit p: Parameters): TLNode = {
    val user = LazyModule(new TLUserUser(meta, f))
    user.node
  }
}

