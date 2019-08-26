// See LICENSE.SiFive for license details.

package freechips.rocketchip.tilelink

import Chisel._
import chipsalliance.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

/** Synthesizeable unit tests */
import freechips.rocketchip.unittest._

class TestRobin(txns: Int = 128, timeout: Int = 500000)(implicit p: Parameters) extends UnitTest(timeout) {
  val sources = Wire(Vec(6, DecoupledIO(UInt(width=3))))
  val sink = Wire(DecoupledIO(UInt(width=3)))
  val count = RegInit(UInt(0, width=8))

  val lfsr = LFSR16(Bool(true))
  val valid = lfsr(0)
  val ready = lfsr(15)

  sources.zipWithIndex.map { case (z, i) => z.bits := UInt(i) }
  sources(0).valid := valid
  sources(1).valid := Bool(false)
  sources(2).valid := valid
  sources(3).valid := valid
  sources(4).valid := Bool(false)
  sources(5).valid := valid
  sink.ready := ready

  TLArbiter(TLArbiter.roundRobin)(sink, sources.zipWithIndex.map { case (z, i) => (UInt(i), z) }:_*)
  when (sink.fire()) { printf("TestRobin: %d\n", sink.bits) }
  when (!sink.fire()) { printf("TestRobin: idle (%d %d)\n", valid, ready) }

  count := count + UInt(1)
  io.finished := count >= UInt(txns)
}
