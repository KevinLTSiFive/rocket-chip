// See LICENSE.SiFive for license details.

package freechips.rocketchip.tilelink

import Chisel._
import chipsalliance.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._
import scala.math.{min,max}

/** Synthesizeable unit tests */
import freechips.rocketchip.unittest._

class TLRAMWidthWidget(first: Int, second: Int, txns: Int)(implicit p: Parameters) extends LazyModule {
  val fuzz = LazyModule(new TLFuzzer(txns))
  val model = LazyModule(new TLRAMModel("WidthWidget"))
  val ram  = LazyModule(new TLRAM(AddressSet(0x0, 0x3ff)))

  (ram.node
    := TLDelayer(0.1)
    := TLFragmenter(4, 256)
    := TLWidthWidget(second)
    := TLWidthWidget(first)
    := TLDelayer(0.1)
    := model.node
    := fuzz.node)

  lazy val module = new LazyModuleImp(this) with UnitTestModule {
    io.finished := fuzz.module.io.finished
  }
}

class TLRAMWidthWidgetTest(little: Int, big: Int, txns: Int = 5000, timeout: Int = 500000)(implicit p: Parameters) extends UnitTest(timeout) {
  val dut = Module(LazyModule(new TLRAMWidthWidget(little,big,txns)).module)
  io.finished := dut.io.finished
}
