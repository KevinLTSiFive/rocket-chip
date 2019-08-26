// See LICENSE.SiFive for license details.

package freechips.rocketchip.tilelink

import Chisel._
import chipsalliance.rocketchip.config._
import freechips.rocketchip.diplomacy._

/** Synthesizeable unit tests */
import freechips.rocketchip.unittest._

class TLRAMXbar(nManagers: Int, txns: Int)(implicit p: Parameters) extends LazyModule {
  val fuzz = LazyModule(new TLFuzzer(txns))
  val model = LazyModule(new TLRAMModel("Xbar"))
  val xbar = LazyModule(new TLXbar)

  xbar.node := TLDelayer(0.1) := model.node := fuzz.node
  (0 until nManagers) foreach { n =>
    val ram  = LazyModule(new TLRAM(AddressSet(0x0+0x400*n, 0x3ff)))
    ram.node := TLFragmenter(4, 256) := TLDelayer(0.1) := xbar.node
  }

  lazy val module = new LazyModuleImp(this) with UnitTestModule {
    io.finished := fuzz.module.io.finished
  }
}

class TLRAMXbarTest(nManagers: Int, txns: Int = 5000, timeout: Int = 500000)(implicit p: Parameters) extends UnitTest(timeout) {
  val dut = Module(LazyModule(new TLRAMXbar(nManagers,txns)).module)
  io.finished := dut.io.finished
}

class TLMulticlientXbar(nManagers: Int, nClients: Int, txns: Int)(implicit p: Parameters) extends LazyModule {
  val xbar = LazyModule(new TLXbar)

  val fuzzers = (0 until nClients) map { n =>
    val fuzz = LazyModule(new TLFuzzer(txns))
    xbar.node := TLDelayer(0.1) := fuzz.node
    fuzz
  }

  (0 until nManagers) foreach { n =>
    val ram = LazyModule(new TLRAM(AddressSet(0x0+0x400*n, 0x3ff)))
    ram.node := TLFragmenter(4, 256) := TLDelayer(0.1) := xbar.node
  }

  lazy val module = new LazyModuleImp(this) with UnitTestModule {
    io.finished := fuzzers.last.module.io.finished
  }
}

class TLMulticlientXbarTest(nManagers: Int, nClients: Int, txns: Int = 5000, timeout: Int = 500000)(implicit p: Parameters) extends UnitTest(timeout) {
  val dut = Module(LazyModule(new TLMulticlientXbar(nManagers, nClients, txns)).module)
  io.finished := dut.io.finished
}
