// See LICENSE.SiFive for license details.

package freechips.rocketchip.tilelink

import Chisel._
import chipsalliance.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.subsystem.CrossingWrapper
import freechips.rocketchip.util._
import freechips.rocketchip.util.property._

/** Synthesizeable unit tests */
import freechips.rocketchip.unittest._

class TLRAMAsyncCrossing(txns: Int, params: AsynchronousCrossing = AsynchronousCrossing())(implicit p: Parameters) extends LazyModule {
  val model = LazyModule(new TLRAMModel("AsyncCrossing"))
  val fuzz = LazyModule(new TLFuzzer(txns))
  val island = LazyModule(new CrossingWrapper(params))
  val ram  = island { LazyModule(new TLRAM(AddressSet(0x0, 0x3ff))) }

  island.crossTLIn(ram.node) := TLFragmenter(4, 256) := TLDelayer(0.1) := model.node := fuzz.node

  lazy val module = new LazyModuleImp(this) with UnitTestModule {
    io.finished := fuzz.module.io.finished

    // Shove the RAM into another clock domain
    val clocks = Module(new Pow2ClockDivider(2))
    island.module.clock := clocks.io.clock_out
  }
}

class TLRAMAsyncCrossingTest(txns: Int = 5000, timeout: Int = 500000)(implicit p: Parameters) extends UnitTest(timeout) {
  val dut_wide   = Module(LazyModule(new TLRAMAsyncCrossing(txns)).module)
  val dut_narrow = Module(LazyModule(new TLRAMAsyncCrossing(txns, AsynchronousCrossing(safe = false, narrow = true))).module)
  io.finished := dut_wide.io.finished && dut_narrow.io.finished
}
