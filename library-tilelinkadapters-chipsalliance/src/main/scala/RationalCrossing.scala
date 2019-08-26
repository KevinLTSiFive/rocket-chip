// See LICENSE.SiFive for license details.

// If you know two clocks are related with a N:1 or 1:N relationship, you
// can cross the clock domains with lower latency than an AsyncQueue.
// This clock crossing behaves almost identically to a TLBuffer(2):
//   - It adds one cycle latency to each clock domain.
//   - All outputs of TLRational are registers (bits, valid, and ready).
//   - It costs 3*bits registers as opposed to 2*bits in a TLBuffer(2)

package freechips.rocketchip.tilelink

import Chisel._
import chipsalliance.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

class TLRationalCrossingSource(implicit p: Parameters) extends LazyModule
{
  val node = TLRationalSourceNode()

  lazy val module = new LazyModuleImp(this) {
    (node.in zip node.out) foreach { case ((in, edgeIn), (out, edgeOut)) =>
      val bce = edgeIn.manager.anySupportAcquireB && edgeIn.client.anySupportProbe
      val direction = edgeOut.manager.direction

      out.a <> ToRational(in.a, direction)
      in.d <> FromRational(out.d, direction.flip)

      if (bce) {
        in.b <> FromRational(out.b, direction.flip)
        out.c <> ToRational(in.c, direction)
        out.e <> ToRational(in.e, direction)
      } else {
        in.b.valid   := Bool(false)
        in.c.ready   := Bool(true)
        in.e.ready   := Bool(true)
        out.b.ready  := Bool(true)
        out.c.valid  := Bool(false)
        out.e.valid  := Bool(false)
        out.b.sink   := UInt(0)
        out.c.source := UInt(0)
        out.e.source := UInt(0)
      }
    }
  }
}

class TLRationalCrossingSink(direction: RationalDirection = Symmetric)(implicit p: Parameters) extends LazyModule
{
  val node = TLRationalSinkNode(direction)

  lazy val module = new LazyModuleImp(this) {
    (node.in zip node.out) foreach { case ((in, edgeIn), (out, edgeOut)) =>
      val bce = edgeOut.manager.anySupportAcquireB && edgeOut.client.anySupportProbe
      val direction = edgeIn.manager.direction

      out.a <> FromRational(in.a, direction)
      in.d <> ToRational(out.d, direction.flip)

      if (bce) {
        in.b <> ToRational(out.b, direction.flip)
        out.c <> FromRational(in.c, direction)
        out.e <> FromRational(in.e, direction)
      } else {
        out.b.ready := Bool(true)
        out.c.valid := Bool(false)
        out.e.valid := Bool(false)
        in.b.valid  := Bool(false)
        in.c.ready  := Bool(true)
        in.e.ready  := Bool(true)
        in.b.source := UInt(0)
        in.c.sink   := UInt(0)
        in.e.sink   := UInt(0)
      }
    }
  }
}

object TLRationalCrossingSource
{
  def apply()(implicit p: Parameters) =
  {
    val rsource = LazyModule(new TLRationalCrossingSource)
    rsource.node
  }
}

object TLRationalCrossingSink
{
  def apply(direction: RationalDirection = Symmetric)(implicit p: Parameters) =
  {
    val rsink = LazyModule(new TLRationalCrossingSink(direction))
    rsink.node
  }
}

@deprecated("TLRationalCrossing is fragile. Use TLRationalCrossingSource and TLRationalCrossingSink", "rocket-chip 1.2")
class TLRationalCrossing(direction: RationalDirection = Symmetric)(implicit p: Parameters) extends LazyModule
{
  val source = LazyModule(new TLRationalCrossingSource)
  val sink = LazyModule(new TLRationalCrossingSink(direction))
  val node = NodeHandle(source.node, sink.node)

  sink.node := source.node

  lazy val module = new LazyModuleImp(this) {
    val io = IO(new Bundle {
      val in_clock  = Clock(INPUT)
      val in_reset  = Bool(INPUT)
      val out_clock = Clock(INPUT)
      val out_reset = Bool(INPUT)
    })

    source.module.clock := io.in_clock
    source.module.reset := io.in_reset
    sink.module.clock := io.out_clock
    sink.module.reset := io.out_reset
  }
}

