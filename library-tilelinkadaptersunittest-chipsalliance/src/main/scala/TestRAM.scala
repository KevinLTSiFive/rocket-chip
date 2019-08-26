/** Synthesizeable unit testing */
package freechips.rocketchip.devices.tilelink

import Chisel._
import chipsalliance.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util._
import freechips.rocketchip.unittest._

class TLRAMZeroDelay(ramBeatBytes: Int, txns: Int)(implicit p: Parameters) extends LazyModule {
  val fuzz = LazyModule(new TLFuzzer(txns))
  val model = LazyModule(new TLRAMModel("ZeroDelay"))
  val ram  = LazyModule(new TLTestRAM(AddressSet(0x0, 0x3ff), beatBytes = ramBeatBytes))

  ram.node := TLDelayer(0.25) := model.node := fuzz.node

  lazy val module = new LazyModuleImp(this) with UnitTestModule {
    io.finished := fuzz.module.io.finished
  }
}

class TLRAMZeroDelayTest(ramBeatBytes: Int, txns: Int = 5000, timeout: Int = 500000)(implicit p: Parameters) extends UnitTest(timeout) {
  val dut = Module(LazyModule(new TLRAMZeroDelay(ramBeatBytes, txns)).module)
  io.finished := dut.io.finished
}
